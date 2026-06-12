package com.mts.online_shop.camunda;

import com.mts.online_shop.repository.UserRepository;
import com.mts.online_shop.security.XmlUserDetailsService;
import org.camunda.bpm.engine.AuthorizationService;
import org.camunda.bpm.engine.IdentityService;
import org.camunda.bpm.engine.authorization.Authorization;
import org.camunda.bpm.engine.authorization.Permissions;
import org.camunda.bpm.engine.authorization.Resources;
import org.camunda.bpm.engine.identity.Group;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Shop users (DB / users.xml / register) ↔ Camunda Identity.
 */
@Service
public class CamundaIdentityService {

    private static final Logger log = LoggerFactory.getLogger(CamundaIdentityService.class);
    private static final String CAMUNDA_ADMIN_GROUP = "camunda-admin";
    private static final List<String> ALL_WEBAPP_IDS = List.of("welcome", "tasklist", "cockpit", "admin");
    private static final List<String> USER_WEBAPP_IDS = List.of("welcome", "tasklist");
    private static final List<String> ADMIN_WEBAPP_IDS = List.of("welcome", "tasklist", "cockpit", "admin");

    private final String builtInAdminId;
    private final String builtInAdminPassword;

    private final IdentityService identityService;
    private final AuthorizationService authorizationService;
    private final XmlUserDetailsService xmlUserDetailsService;
    private final UserRepository userRepository;
    private final JdbcTemplate jdbcTemplate;
    private final CamundaIdentityPasswordWriter passwordWriter;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public CamundaIdentityService(IdentityService identityService,
                                  AuthorizationService authorizationService,
                                  XmlUserDetailsService xmlUserDetailsService,
                                  UserRepository userRepository,
                                  JdbcTemplate jdbcTemplate,
                                  CamundaIdentityPasswordWriter passwordWriter,
                                  @Value("${camunda.bpm.admin-user.id:demo}") String builtInAdminId,
                                  @Value("${camunda.bpm.admin-user.password:demo}") String builtInAdminPassword) {
        this.identityService = identityService;
        this.authorizationService = authorizationService;
        this.xmlUserDetailsService = xmlUserDetailsService;
        this.userRepository = userRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.passwordWriter = passwordWriter;
        this.builtInAdminId = builtInAdminId;
        this.builtInAdminPassword = builtInAdminPassword;
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void syncAllShopUsers() {
        xmlUserDetailsService.syncUsersXmlFromDatabase();
        userRepository.findAll().forEach(user -> {
            if (user.getLogin() != null && !user.getLogin().isBlank()) {
                xmlUserDetailsService.ensureUserInXmlFromDatabase(user.getLogin());
            }
            syncFromDatabaseUser(user);
        });
        xmlUserDetailsService.getAllUsers().values().forEach(this::syncFromUserDetails);
        syncDefaultShopAccounts();
        syncBuiltInAdminUser();
        unlockAllIdentityUsers();
        log.info("Camunda identity synced ({} DB accounts), shop + built-in admin users unlocked",
                userRepository.count());
    }

    public void unlockAllIdentityUsers() {
        unlockAllShopUsers();
        unlockAllCamundaUsers();
    }

    public void syncDefaultShopAccounts() {
        if (xmlUserDetailsService.userExists("admin")) {
            syncWithPlainPassword("admin", "admin", List.of("admin"));
        }
        if (xmlUserDetailsService.userExists("user")) {
            syncWithPlainPassword("user", "user", List.of("user"));
        }
    }

    public boolean isBuiltInAdminCredentials(String login, String plainPassword) {
        if (login == null || plainPassword == null) {
            return false;
        }
        return builtInAdminId.trim().equalsIgnoreCase(login.trim())
                && builtInAdminPassword.equals(plainPassword);
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void syncBuiltInAdminUser() {
        String userId = builtInAdminId.trim().toLowerCase(Locale.ROOT);
        syncWithPlainPassword(userId, builtInAdminPassword, List.of(CAMUNDA_ADMIN_GROUP));
        ensureCamundaAdminGroupMembership(userId);
        log.info("Camunda built-in admin user '{}' password synced", userId);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void syncOnLogin(String login, String plainPassword) {
        if (plainPassword == null || plainPassword.isBlank()) {
            log.warn("Skip Camunda sync on login for '{}': empty password", login);
            return;
        }
        String userId = login.trim().toLowerCase(Locale.ROOT);
        passwordWriter.unlockUser(userId);
        if (isBuiltInAdminCredentials(userId, plainPassword)) {
            syncBuiltInAdminUser();
            return;
        }
        Collection<String> roles = resolveShopRoles(userId);
        if (roles.isEmpty()) {
            log.warn("Skip Camunda sync on login for '{}': shop account not found", userId);
            return;
        }
        syncWithPlainPassword(userId, plainPassword, roles);
        String hash = resolveBcryptHashForSync(userId, lookupPasswordHash(userId), plainPassword);
        if (CamundaBcryptPasswordEncryptor.isBcryptHash(hash)) {
            xmlUserDetailsService.persistPasswordHashToXml(userId, hash);
        }
        log.info("Camunda identity synced on login for '{}'", userId);
    }

    /** Пересинхронизировать Camunda Identity после смены роли в БД. */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void resyncShopUser(String login) {
        if (login == null || login.isBlank()) {
            return;
        }
        String userId = login.trim().toLowerCase(Locale.ROOT);
        userRepository.findByLoginIgnoreCaseOrEmailIgnoreCase(userId, userId).ifPresent(dbUser -> {
            xmlUserDetailsService.syncRoleForLogin(userId, dbUser.getRole());
            syncFromDatabaseUser(dbUser);
            unlockUser(userId);
            log.info("Camunda identity re-synced for '{}'", userId);
        });
    }

    private Collection<String> resolveShopRoles(String userId) {
        xmlUserDetailsService.ensureUserInXmlFromDatabase(userId);
        if (xmlUserDetailsService.userExists(userId)) {
            try {
                return extractRoles(xmlUserDetailsService.loadUserByUsername(userId));
            } catch (Exception ex) {
                log.debug("Could not load '{}' from users.xml: {}", userId, ex.getMessage());
            }
        }
        return userRepository.findByLoginIgnoreCaseOrEmailIgnoreCase(userId, userId)
                .map(user -> List.of(normalizeRole(user.getRole())))
                .orElse(List.of());
    }

    private String lookupPasswordHash(String userId) {
        if (xmlUserDetailsService.userExists(userId)) {
            try {
                return xmlUserDetailsService.loadUserByUsername(userId).getPassword();
            } catch (Exception ignored) {
                // fall through to DB
            }
        }
        return userRepository.findByLoginIgnoreCaseOrEmailIgnoreCase(userId, userId)
                .map(com.mts.online_shop.model.User::getPasswordHash)
                .orElse("");
    }

    public void syncAllFromXml() {
        xmlUserDetailsService.reloadFromDisk();
        xmlUserDetailsService.getAllUsers().values().forEach(this::syncFromUserDetails);
        log.info("Camunda identity synced from users.xml ({} accounts)", xmlUserDetailsService.getAllUsers().size());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void syncRegisteredUser(String login, String plainPassword, Collection<String> roles) {
        if (plainPassword == null || plainPassword.isBlank()) {
            log.warn("Skip Camunda sync for new user '{}': empty password", login);
            return;
        }
        String userId = login.trim().toLowerCase(Locale.ROOT);
        xmlUserDetailsService.ensureUserInXmlFromDatabase(userId);
        syncWithPlainPassword(userId, plainPassword, roles);
        log.info("Camunda identity synced for new user '{}'", userId);
    }

    private void syncFromDatabaseUser(com.mts.online_shop.model.User user) {
        if (user.getLogin() == null || user.getLogin().isBlank()) {
            return;
        }
        String hash = user.getPasswordHash();
        if (!CamundaBcryptPasswordEncryptor.isBcryptHash(hash)) {
            log.warn("Skip Camunda sync for '{}': DB password_hash is not BCrypt", user.getLogin());
            return;
        }
        syncWithBcryptHash(user.getLogin(), hash, List.of(normalizeRole(user.getRole())));
    }

    private void syncFromUserDetails(UserDetails userDetails) {
        String passwordHash = userDetails.getPassword();
        if (!CamundaBcryptPasswordEncryptor.isBcryptHash(passwordHash)) {
            log.warn("Skip Camunda sync for '{}': users.xml password is not BCrypt", userDetails.getUsername());
            return;
        }
        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(role -> role.replace("ROLE_", ""))
                .map(this::normalizeRole)
                .collect(Collectors.toList());
        syncWithBcryptHash(userDetails.getUsername(), passwordHash, roles);
    }

    private String resolveBcryptHashForSync(String login, String xmlOrCachedHash, String plainPassword) {
        if (CamundaBcryptPasswordEncryptor.isBcryptHash(xmlOrCachedHash)) {
            return xmlOrCachedHash;
        }
        String dbHash = userRepository.findByLoginIgnoreCaseOrEmailIgnoreCase(login, login)
                .map(com.mts.online_shop.model.User::getPasswordHash)
                .orElse(null);
        if (CamundaBcryptPasswordEncryptor.isBcryptHash(dbHash)) {
            return dbHash;
        }
        String templateHash = xmlUserDetailsService.lookupTemplatePasswordHashForUser(login);
        if (CamundaBcryptPasswordEncryptor.isBcryptHash(templateHash)
                && plainPassword != null
                && passwordEncoder.matches(plainPassword, templateHash)) {
            return templateHash;
        }
        if (plainPassword != null && !plainPassword.isBlank()) {
            return passwordEncoder.encode(plainPassword);
        }
        return xmlOrCachedHash;
    }

    private List<String> extractRoles(UserDetails userDetails) {
        return userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(role -> role.replace("ROLE_", ""))
                .map(this::normalizeRole)
                .collect(Collectors.toList());
    }

    private void syncWithPlainPassword(String login, String plainPassword, Collection<String> roles) {
        String userId = login.toLowerCase(Locale.ROOT);
        persistCamundaPlainPassword(userId, plainPassword);
        effectiveCamundaGroups(roles).forEach(role -> ensureGroupMembership(userId, role));
        ensureDirectWebappAccess(userId, roles);
    }

    /**
     * Startup / bulk sync when only the shop BCrypt hash is available (no plain password).
     * Camunda salts passwords on save; importing a hash via Identity API would break login.
     * With {@code salt_ = NULL}, checkPassword uses the plain password unchanged (Camunda &lt; 7.7 behaviour).
     */
    private void syncWithBcryptHash(String login, String bcryptHash, Collection<String> roles) {
        if (!CamundaBcryptPasswordEncryptor.isBcryptHash(bcryptHash)) {
            log.warn("Skip Camunda sync for '{}': password is not a valid BCrypt hash", login);
            return;
        }
        String userId = login.toLowerCase(Locale.ROOT);
        String displayName = userRepository.findByLoginIgnoreCaseOrEmailIgnoreCase(userId, userId)
                .map(com.mts.online_shop.model.User::getName)
                .orElse(userId);
        passwordWriter.upsertShopUser(
                userId,
                CamundaBcryptPasswordEncryptor.unwrapBcryptHash(bcryptHash),
                displayName);
        effectiveCamundaGroups(roles).forEach(role -> ensureGroupMembership(userId, role));
        ensureDirectWebappAccess(userId, roles);
    }

    /**
     * Администратор наследует группу {@code user}, чтобы работать с USER-процессами в Tasklist.
     */
    private Set<String> effectiveCamundaGroups(Collection<String> roles) {
        Set<String> groups = roles.stream()
                .map(this::normalizeRole)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (groups.contains("admin") || groups.contains(CAMUNDA_ADMIN_GROUP)) {
            groups.add("user");
        }
        if (groups.contains(CAMUNDA_ADMIN_GROUP)) {
            groups.add("admin");
        }
        return groups;
    }

    /**
     * Пересинхронизирует персональные APPLICATION-гранты после смены групповых authorizations.
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void reconcileAllUserWebappAccess() {
        identityService.createUserQuery().list().forEach(user -> {
            String userId = user.getId();
            List<String> roles = identityService.createGroupQuery()
                    .groupMember(userId)
                    .list()
                    .stream()
                    .map(Group::getId)
                    .collect(Collectors.toList());
            ensureDirectWebappAccess(userId, roles);
        });
        log.info("Camunda per-user webapp ACCESS reconciled for {} account(s)",
                identityService.createUserQuery().count());
    }

    /**
     * Store the same BCrypt hash Spring Security uses ({@code salt_ = NULL}).
     * Avoids IdentityService.saveUser, which salts passwords and breaks shop credentials.
     */
    private void persistCamundaPlainPassword(String userId, String plainPassword) {
        String bareHash = resolveBareBcryptHash(userId, plainPassword);
        String displayName = userRepository.findByLoginIgnoreCaseOrEmailIgnoreCase(userId, userId)
                .map(com.mts.online_shop.model.User::getName)
                .orElse(userId);
        passwordWriter.upsertShopUser(userId, bareHash, displayName);
        logPasswordDiagnostics(userId, plainPassword);
    }

    private String resolveBareBcryptHash(String userId, String plainPassword) {
        if (xmlUserDetailsService.userExists(userId)) {
            try {
                String hash = xmlUserDetailsService.loadUserByUsername(userId).getPassword();
                if (matchesPlainPassword(plainPassword, hash)) {
                    return CamundaBcryptPasswordEncryptor.unwrapBcryptHash(hash);
                }
            } catch (Exception ex) {
                log.debug("Could not resolve shop BCrypt from XML for '{}': {}", userId, ex.getMessage());
            }
        }
        return userRepository.findByLoginIgnoreCaseOrEmailIgnoreCase(userId, userId)
                .map(com.mts.online_shop.model.User::getPasswordHash)
                .filter(hash -> matchesPlainPassword(plainPassword, hash))
                .map(CamundaBcryptPasswordEncryptor::unwrapBcryptHash)
                .orElseGet(() -> passwordEncoder.encode(plainPassword));
    }

    private boolean matchesPlainPassword(String plainPassword, String storedHash) {
        if (!CamundaBcryptPasswordEncryptor.isBcryptHash(storedHash)) {
            return false;
        }
        return passwordEncoder.matches(plainPassword, CamundaBcryptPasswordEncryptor.unwrapBcryptHash(storedHash));
    }

    private void logPasswordDiagnostics(String userId, String plainPassword) {
        try {
            String pwd = jdbcTemplate.queryForObject(
                    "SELECT pwd_ FROM act_id_user WHERE id_ = ?", String.class, userId);
            String salt = jdbcTemplate.queryForObject(
                    "SELECT salt_ FROM act_id_user WHERE id_ = ?", String.class, userId);
            boolean camundaOk = identityService.checkPassword(userId, plainPassword);
            log.info(
                    "Camunda password sync verify for '{}': stored_prefix={}, salt_blank={}, checkPassword={}",
                    userId,
                    pwd != null && pwd.length() > 12 ? pwd.substring(0, 12) + "..." : pwd,
                    salt == null || salt.isBlank(),
                    camundaOk);
        } catch (Exception ex) {
            log.warn("Camunda password verify failed for '{}': {}", userId, ex.getMessage());
        }
    }

    private void unlockAllShopUsers() {
        userRepository.findAll().stream()
                .map(com.mts.online_shop.model.User::getLogin)
                .filter(login -> login != null && !login.isBlank())
                .map(login -> login.toLowerCase(Locale.ROOT))
                .forEach(this::unlockUser);
        xmlUserDetailsService.getAllUsers().keySet().stream()
                .map(String::toLowerCase)
                .forEach(this::unlockUser);
    }

    private void unlockUser(String userId) {
        passwordWriter.unlockUser(userId);
    }

    private void unlockAllCamundaUsers() {
        int clearedSalt = passwordWriter.clearLegacySalts();
        if (clearedSalt > 0) {
            log.info("Cleared legacy Camunda salt for {} user(s) (shop BCrypt compatibility)", clearedSalt);
        }
        int updated = passwordWriter.unlockAllLockedUsers();
        if (updated > 0) {
            log.info("Unlocked {} Camunda identity user(s) after failed login attempts", updated);
        }
    }

    private void ensureCamundaAdminGroupMembership(String userId) {
        Group group = identityService.createGroupQuery().groupId(CAMUNDA_ADMIN_GROUP).singleResult();
        if (group == null) {
            group = identityService.newGroup(CAMUNDA_ADMIN_GROUP);
            group.setName("camunda BPM Administrators");
            group.setType("workflow");
            identityService.saveGroup(group);
        }
        if (identityService.createGroupQuery()
                .groupId(CAMUNDA_ADMIN_GROUP)
                .groupMember(userId)
                .singleResult() == null) {
            identityService.createMembership(userId, CAMUNDA_ADMIN_GROUP);
        }
    }

    private String normalizeRole(String role) {
        if (role == null || role.isBlank()) {
            return "user";
        }
        String normalized = role.trim().toLowerCase(Locale.ROOT);
        if ("customer".equals(normalized)) {
            return "user";
        }
        return normalized;
    }

    /** Login/{app} returns 401 if the user lacks APPLICATION ACCESS (even with a valid password). */
    private void ensureDirectWebappAccess(String userId, Collection<String> roles) {
        List<String> allowedApps = resolveWebappsForRoles(roles);
        for (String app : ALL_WEBAPP_IDS) {
            if (!allowedApps.contains(app)) {
                revokeUserWebappAccess(userId, app);
            }
        }
        revokeUserWildcardWebappAccess(userId, roles);
        for (String app : allowedApps) {
            if (authorizationService.createAuthorizationQuery()
                    .authorizationType(Authorization.AUTH_TYPE_GRANT)
                    .userIdIn(userId)
                    .resourceType(Resources.APPLICATION)
                    .resourceId(app)
                    .count() > 0) {
                continue;
            }
            Authorization authorization = authorizationService.createNewAuthorization(Authorization.AUTH_TYPE_GRANT);
            authorization.setUserId(userId);
            authorization.setResource(Resources.APPLICATION);
            authorization.setResourceId(app);
            authorization.addPermission(Permissions.ACCESS);
            authorizationService.saveAuthorization(authorization);
            log.info("Granted Camunda webapp '{}' ACCESS for user '{}'", app, userId);
        }
    }

    private List<String> resolveWebappsForRoles(Collection<String> roles) {
        boolean camundaAdmin = roles.stream().map(this::normalizeRole)
                .anyMatch(CAMUNDA_ADMIN_GROUP::equals);
        boolean shopAdmin = roles.stream().map(this::normalizeRole)
                .anyMatch("admin"::equals);
        if (camundaAdmin || shopAdmin) {
            return ADMIN_WEBAPP_IDS;
        }
        return USER_WEBAPP_IDS;
    }

    private void revokeUserWebappAccess(String userId, String appId) {
        authorizationService.createAuthorizationQuery()
                .authorizationType(Authorization.AUTH_TYPE_GRANT)
                .userIdIn(userId)
                .resourceType(Resources.APPLICATION)
                .resourceId(appId)
                .list()
                .forEach(existing -> authorizationService.deleteAuthorization(existing.getId()));
    }

    private void revokeUserWildcardWebappAccess(String userId, Collection<String> roles) {
        boolean camundaAdmin = roles.stream().map(this::normalizeRole)
                .anyMatch(CAMUNDA_ADMIN_GROUP::equals);
        if (camundaAdmin) {
            return;
        }
        authorizationService.createAuthorizationQuery()
                .authorizationType(Authorization.AUTH_TYPE_GRANT)
                .userIdIn(userId)
                .resourceType(Resources.APPLICATION)
                .resourceId("*")
                .list()
                .forEach(existing -> authorizationService.deleteAuthorization(existing.getId()));
    }

    private void ensureGroupMembership(String userId, String role) {
        Group group = identityService.createGroupQuery().groupId(role).singleResult();
        if (group == null) {
            group = identityService.newGroup(role);
            group.setName(role.toUpperCase(Locale.ROOT));
            group.setType("application");
            identityService.saveGroup(group);
        }
        if (identityService.createGroupQuery()
                .groupId(role)
                .groupMember(userId)
                .singleResult() == null) {
            identityService.createMembership(userId, role);
        }
        removeLegacyGroupMembership(userId);
    }

    /** Authorizations use lowercase groups ({@code user}, {@code admin}); drop legacy uppercase memberships. */
    private void removeLegacyGroupMembership(String userId) {
        for (String legacyGroupId : List.of("USER", "ADMIN")) {
            if (identityService.createGroupQuery()
                    .groupId(legacyGroupId)
                    .groupMember(userId)
                    .singleResult() != null) {
                identityService.deleteMembership(userId, legacyGroupId);
                log.info("Removed legacy Camunda group '{}' for user '{}'", legacyGroupId, userId);
            }
        }
    }

}
