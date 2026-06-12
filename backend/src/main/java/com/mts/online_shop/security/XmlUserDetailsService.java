package com.mts.online_shop.security;

import com.mts.online_shop.camunda.CamundaBcryptPasswordEncryptor;
import com.mts.online_shop.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Service
public class XmlUserDetailsService implements UserDetailsService {

    private static final Logger log = LoggerFactory.getLogger(XmlUserDetailsService.class);
    private final Map<String, UserDetails> users = new HashMap<>();
    private final Map<String, Long> usernameToId = new HashMap<>();
    private final PrivilegeService privilegeService;
    private final UserRepository userRepository;
    private Path xmlFilePath;

    public XmlUserDetailsService(PrivilegeService privilegeService, UserRepository userRepository) {
        this.privilegeService = privilegeService;
        this.userRepository = userRepository;
    }

    @PostConstruct
    public void init() {
        try {
            xmlFilePath = UsersXmlLocation.resolve();
            File dataDir = xmlFilePath.getParent().toFile();
            if (!dataDir.exists()) {
                dataDir.mkdirs();
            }

            UsersXmlLocation.migrateLegacyIfNeeded(xmlFilePath);

            if (!xmlFilePath.toFile().exists()) {
                log.info("Copying users.xml from resources to {}", xmlFilePath);
                ClassPathResource resource = new ClassPathResource(UsersXmlLocation.XML_FILE_NAME);
                try (InputStream is = resource.getInputStream()) {
                    Files.copy(is, xmlFilePath);
                }
            }

            UsersXmlLocation.publish(xmlFilePath);
            log.info("Using users.xml at {}", xmlFilePath.toAbsolutePath());
            loadUsersFromXml();
        } catch (Exception e) {
            log.error("Failed to initialize XML user storage", e);
            throw new RuntimeException("Failed to initialize XML user storage", e);
        }
    }

    /** Перечитать users.xml с диска (перед синхронизацией Camunda после перезапуска). */
    public void reloadFromDisk() {
        loadUsersFromXml();
    }

    public Path getUsersXmlPath() {
        return xmlFilePath;
    }

    private void loadUsersFromXml() {
        try {
            users.clear();
            
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(false);
            DocumentBuilder builder = factory.newDocumentBuilder();
            
            Document doc = builder.parse(xmlFilePath.toFile());
            doc.getDocumentElement().normalize();
            
            NodeList userNodes = doc.getElementsByTagName("user");
            log.info("Loading {} users from XML", userNodes.getLength());
            
            for (int i = 0; i < userNodes.getLength(); i++) {
                Element userElement = (Element) userNodes.item(i);
                
                String username = userElement.getAttribute("username");
                String userIdStr = userElement.getAttribute("id");
                Long userId = (userIdStr != null && !userIdStr.trim().isEmpty()) ? Long.parseLong(userIdStr.trim()) : null;
                String password = resolvePassword(username, userElement.getAttribute("password"));
                boolean enabled = Boolean.parseBoolean(userElement.getAttribute("enabled"));
                
                // Parse roles
                List<String> roles = new ArrayList<>();
                NodeList rolesList = userElement.getElementsByTagName("role");
                for (int j = 0; j < rolesList.getLength(); j++) {
                    roles.add(rolesList.item(j).getTextContent().trim().toUpperCase());
                }
                
                // Convert roles to authorities
                List<SimpleGrantedAuthority> authorities = new ArrayList<>();
                for (String role : roles) {
                    authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
                }
                
                // Create XmlUserPrincipal with userId
                UserDetails userDetails = new XmlUserPrincipal(
                        username, // use username for authentication
                        password,
                        enabled,
                        authorities,
                        userId
                );
                
                users.put(username.toLowerCase(), userDetails);
                if (userId != null) {
                    usernameToId.put(username.toLowerCase(), userId);
                }
            }
            
            log.info("Successfully loaded {} users from XML", users.size());
            
        } catch (Exception e) {
            log.error("Failed to load users from XML", e);
            throw new RuntimeException("Failed to load users from XML", e);
        }
    }

    private String resolvePassword(String username, String xmlPassword) {
        if (CamundaBcryptPasswordEncryptor.isBcryptHash(xmlPassword)) {
            return xmlPassword;
        }
        String dbHash = lookupDatabasePasswordHash(username);
        if (CamundaBcryptPasswordEncryptor.isBcryptHash(dbHash)) {
            log.warn("User '{}' has missing or invalid password in users.xml — using BCrypt hash from database",
                    username);
            return dbHash;
        }
        String templateHash = lookupTemplatePasswordHash(username);
        if (CamundaBcryptPasswordEncryptor.isBcryptHash(templateHash)) {
            log.warn("User '{}' has missing or invalid password in users.xml — using BCrypt hash from template",
                    username);
            return templateHash;
        }
        return xmlPassword != null ? xmlPassword : "";
    }

    public void saveUser(String username, String password, List<String> roles) {
        saveUserWithId(username, password, roles, allocateNextUserId(0L));
    }

    public void saveUserWithId(String username, String password, List<String> roles, Long newUserId) {
        try {
            if (users.containsKey(username.toLowerCase())) {
                throw new RuntimeException("User already exists: " + username);
            }

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(xmlFilePath.toFile());

            Element root = doc.getDocumentElement();
            
            // Create new user element
            Element newUser = doc.createElement("user");
            newUser.setAttribute("id", newUserId.toString());
            newUser.setAttribute("username", username);
            newUser.setAttribute("password", password);
            newUser.setAttribute("enabled", "true");
            
            // Create roles element
            Element rolesElement = doc.createElement("roles");
            for (String role : roles) {
                Element roleEl = doc.createElement("role");
                roleEl.setTextContent(role);
                rolesElement.appendChild(roleEl);
            }
            newUser.appendChild(rolesElement);
            
            // Add user to document
            root.appendChild(newUser);
            
            // Save document
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(javax.xml.transform.OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
            DOMSource source = new DOMSource(doc);
            StreamResult result = new StreamResult(xmlFilePath.toFile());
            transformer.transform(source, result);
            
            // Reload users
            loadUsersFromXml();
            
            log.info("Saved new user: {} with ID {} and roles {}", username, newUserId, roles);
            
        } catch (Exception e) {
            log.error("Failed to save user to XML", e);
            throw new RuntimeException("Failed to save user to XML", e);
        }
    }
    
    public Long allocateNextUserId(long minIdInclusive) {
        Long maxXmlId = usernameToId.values().stream()
                .max(Long::compareTo)
                .orElse(0L);
        return Math.max(maxXmlId, minIdInclusive) + 1;
    }

    public boolean userExists(String username) {
        return users.containsKey(username.toLowerCase());
    }

    public void removeUser(String username) {
        String key = username.toLowerCase();
        if (!users.containsKey(key)) {
            return;
        }
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(xmlFilePath.toFile());
            NodeList userNodes = doc.getElementsByTagName("user");
            for (int i = userNodes.getLength() - 1; i >= 0; i--) {
                Element userElement = (Element) userNodes.item(i);
                if (key.equals(userElement.getAttribute("username").toLowerCase())) {
                    userElement.getParentNode().removeChild(userElement);
                }
            }
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(javax.xml.transform.OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
            transformer.transform(new DOMSource(doc), new StreamResult(xmlFilePath.toFile()));
            loadUsersFromXml();
            log.info("Removed user from XML: {}", username);
        } catch (Exception e) {
            log.error("Failed to remove user from XML: {}", username, e);
            throw new RuntimeException("Failed to remove user from XML: " + username, e);
        }
    }

    public Long getUserIdByUsername(String username) {
        return usernameToId.get(username.toLowerCase());
    }

    /** ID пользователя магазина из БД (источник истины для корзины и заказов). */
    public Optional<Long> findDatabaseUserIdByLogin(String username) {
        if (username == null || username.isBlank()) {
            return Optional.empty();
        }
        String normalized = username.trim().toLowerCase(Locale.ROOT);
        return userRepository.findByLoginIgnoreCaseOrEmailIgnoreCase(normalized, normalized)
                .map(com.mts.online_shop.model.User::getId);
    }

    /**
     * ID из БД; при отсутствии учётки создаёт её из users.xml (admin/user по умолчанию).
     */
    @Transactional
    public Optional<Long> ensureDatabaseUserIdByLogin(String username) {
        Optional<Long> existing = findDatabaseUserIdByLogin(username);
        if (existing.isPresent()) {
            return existing;
        }
        provisionDatabaseUserFromXml(username);
        return findDatabaseUserIdByLogin(username);
    }

    @Transactional
    public void provisionDatabaseUserFromXml(String username) {
        if (username == null || username.isBlank()) {
            return;
        }
        String login = username.trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByLoginIgnoreCase(login)) {
            return;
        }
        UserDetails details = users.get(login);
        if (!(details instanceof XmlUserPrincipal principal)) {
            log.warn("Cannot provision '{}': not found in users.xml", login);
            return;
        }
        Long userId = principal.getUserId();
        if (userId == null) {
            log.warn("Cannot provision '{}': no id in users.xml", login);
            return;
        }
        if (userRepository.existsById(userId)) {
            log.warn("Cannot provision '{}': id {} already taken in database", login, userId);
            return;
        }
        String passwordHash = details.getPassword();
        if (!CamundaBcryptPasswordEncryptor.isBcryptHash(passwordHash)) {
            log.warn("Cannot provision '{}': password in users.xml is not BCrypt", login);
            return;
        }
        com.mts.online_shop.model.User dbUser = new com.mts.online_shop.model.User();
        dbUser.setId(userId);
        dbUser.setLogin(login);
        dbUser.setEmail(login + "@localhost");
        dbUser.setName(displayNameForLogin(login));
        dbUser.setPasswordHash(passwordHash);
        dbUser.setRole(primaryRole(details));
        userRepository.save(dbUser);
        log.info("Provisioned database user '{}' with id {}", login, userId);
    }

    /**
     * Создаёт в БД учётки из users.xml, если их ещё нет (admin/user по умолчанию).
     * Нужно для Camunda Tasklist: BPMN берёт userId для корзины из таблицы users.
     */
    @Transactional
    public void syncDatabaseUsersFromXml() {
        for (String login : getAllUsers().keySet()) {
            provisionDatabaseUserFromXml(login);
        }
    }

    private static String displayNameForLogin(String login) {
        if ("admin".equalsIgnoreCase(login)) {
            return "Administrator";
        }
        if ("user".equalsIgnoreCase(login)) {
            return "User";
        }
        if (login.isEmpty()) {
            return login;
        }
        return Character.toUpperCase(login.charAt(0)) + login.substring(1);
    }

    private static String primaryRole(UserDetails details) {
        return details.getAuthorities().stream()
                .map(a -> a.getAuthority())
                .filter(a -> a.startsWith("ROLE_"))
                .map(a -> a.substring(5))
                .findFirst()
                .orElse("USER");
    }

    public Long getMaxUserId() {
        return usernameToId.values().stream()
                .max(Long::compareTo)
                .orElse(0L);
    }

    public Map<String, UserDetails> getAllUsers() {
        return Collections.unmodifiableMap(users);
    }

    /**
     * Синхронизирует runtime users.xml с БД: добавляет отсутствующие учётки и
     * подставляет BCrypt-хеши вместо пустых/невалидных значений password.
     */
    public void syncUsersXmlFromDatabase() {
        syncDatabaseUsersFromXml();
        addMissingDatabaseUsersToXml();
        syncRolesFromDatabaseToXml();
        repairXmlPasswordsFromDatabase();
    }

    /** Добавить в users.xml учётку из БД, если её ещё нет (Swagger register). */
    public void ensureUserInXmlFromDatabase(String login) {
        if (login == null || login.isBlank()) {
            return;
        }
        String key = login.trim().toLowerCase(Locale.ROOT);
        if (userExists(key)) {
            return;
        }
        userRepository.findByLoginIgnoreCaseOrEmailIgnoreCase(key, key).ifPresent(this::addDatabaseUserToXml);
    }

    /** Обновить роль в users.xml по данным БД (например alisa → ADMIN). */
    public void syncRoleForLogin(String login, String role) {
        if (login == null || login.isBlank()) {
            return;
        }
        String key = login.trim().toLowerCase(Locale.ROOT);
        String normalizedRole = role != null && !role.isBlank()
                ? role.trim().toUpperCase(Locale.ROOT)
                : "USER";
        if (!userExists(key)) {
            return;
        }
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(xmlFilePath.toFile());
            NodeList userNodes = doc.getElementsByTagName("user");
            boolean changed = false;
            for (int i = 0; i < userNodes.getLength(); i++) {
                Element userElement = (Element) userNodes.item(i);
                if (!key.equals(userElement.getAttribute("username").toLowerCase(Locale.ROOT))) {
                    continue;
                }
                Element rolesElement = (Element) userElement.getElementsByTagName("roles").item(0);
                if (rolesElement == null) {
                    rolesElement = doc.createElement("roles");
                    userElement.appendChild(rolesElement);
                }
                while (rolesElement.getFirstChild() != null) {
                    rolesElement.removeChild(rolesElement.getFirstChild());
                }
                Element roleEl = doc.createElement("role");
                roleEl.setTextContent(normalizedRole);
                rolesElement.appendChild(roleEl);
                changed = true;
                break;
            }
            if (changed) {
                writeXmlDocument(doc);
                loadUsersFromXml();
                log.info("Synced users.xml role for '{}' -> {}", key, normalizedRole);
            }
        } catch (Exception e) {
            log.error("Failed to sync users.xml role for '{}'", key, e);
        }
    }

    public void syncRolesFromDatabaseToXml() {
        for (com.mts.online_shop.model.User dbUser : userRepository.findAll()) {
            if (dbUser.getLogin() == null || dbUser.getLogin().isBlank()) {
                continue;
            }
            syncRoleForLogin(dbUser.getLogin(), dbUser.getRole());
        }
    }

    /** Дописать в users.xml BCrypt из БД для одного пользователя (после login/sync). */
    public void repairXmlPasswordForUser(String username) {
        if (username == null || username.isBlank() || xmlFilePath == null || !xmlFilePath.toFile().exists()) {
            return;
        }
        String dbHash = lookupDatabasePasswordHash(username);
        if (!CamundaBcryptPasswordEncryptor.isBcryptHash(dbHash)) {
            return;
        }
        persistPasswordHashToXml(username, dbHash);
    }

    /** После Liquibase: дописать в users.xml BCrypt из БД и перечитать файл. */
    public void repairXmlPasswordsFromDatabase() {
        if (xmlFilePath == null || !xmlFilePath.toFile().exists()) {
            return;
        }
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(xmlFilePath.toFile());
            doc.getDocumentElement().normalize();
            NodeList userNodes = doc.getElementsByTagName("user");
            boolean changed = false;
            for (int i = 0; i < userNodes.getLength(); i++) {
                Element userElement = (Element) userNodes.item(i);
                String username = userElement.getAttribute("username");
                String xmlPassword = userElement.getAttribute("password");
                if (CamundaBcryptPasswordEncryptor.isBcryptHash(xmlPassword)) {
                    continue;
                }
                String dbHash = lookupDatabasePasswordHash(username);
                if (!CamundaBcryptPasswordEncryptor.isBcryptHash(dbHash)) {
                    dbHash = lookupTemplatePasswordHash(username);
                }
                if (!CamundaBcryptPasswordEncryptor.isBcryptHash(dbHash)) {
                    log.warn("Cannot repair users.xml for '{}': no BCrypt hash in database or template", username);
                    continue;
                }
                userElement.setAttribute("password", dbHash);
                changed = true;
                log.info("Repaired users.xml password for '{}' from database", username);
            }
            if (changed) {
                writeXmlDocument(doc);
                loadUsersFromXml();
            }
        } catch (Exception e) {
            log.error("Failed to repair users.xml from database", e);
        }
    }

    private void addMissingDatabaseUsersToXml() {
        for (com.mts.online_shop.model.User dbUser : userRepository.findAll()) {
            if (dbUser.getLogin() == null || dbUser.getLogin().isBlank()) {
                continue;
            }
            String login = dbUser.getLogin().trim().toLowerCase(Locale.ROOT);
            if (userExists(login)) {
                continue;
            }
            addDatabaseUserToXml(dbUser);
        }
    }

    private void addDatabaseUserToXml(com.mts.online_shop.model.User dbUser) {
        String login = dbUser.getLogin().trim().toLowerCase(Locale.ROOT);
        String hash = dbUser.getPasswordHash();
        if (!CamundaBcryptPasswordEncryptor.isBcryptHash(hash)) {
            log.warn("Cannot add '{}' to users.xml: DB password_hash is not BCrypt", login);
            return;
        }
        String role = dbUser.getRole() != null && !dbUser.getRole().isBlank()
                ? dbUser.getRole().trim().toUpperCase(Locale.ROOT)
                : "USER";
        Long userId = dbUser.getId() != null ? dbUser.getId() : allocateNextUserId(0L);
        saveUserWithId(login, hash, List.of(role), userId);
        log.info("Added database user '{}' to users.xml", login);
    }

    public void persistPasswordHashToXml(String username, String bcryptHash) {
        if (!CamundaBcryptPasswordEncryptor.isBcryptHash(bcryptHash)) {
            return;
        }
        String key = username.trim().toLowerCase(Locale.ROOT);
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(xmlFilePath.toFile());
            NodeList userNodes = doc.getElementsByTagName("user");
            for (int i = 0; i < userNodes.getLength(); i++) {
                Element userElement = (Element) userNodes.item(i);
                if (!key.equals(userElement.getAttribute("username").toLowerCase(Locale.ROOT))) {
                    continue;
                }
                String xmlPassword = userElement.getAttribute("password");
                if (CamundaBcryptPasswordEncryptor.isBcryptHash(xmlPassword)) {
                    return;
                }
                userElement.setAttribute("password", bcryptHash);
                writeXmlDocument(doc);
                updateCachedPassword(key, bcryptHash);
                log.info("Persisted users.xml password for '{}'", key);
                return;
            }
            log.warn("User '{}' not found in users.xml — cannot persist password hash", key);
        } catch (Exception e) {
            log.error("Failed to persist users.xml password for '{}'", key, e);
        }
    }

    private void updateCachedPassword(String key, String bcryptHash) {
        UserDetails user = users.get(key);
        if (user == null) {
            return;
        }
        users.put(key, new XmlUserPrincipal(
                user.getUsername(),
                bcryptHash,
                user.isEnabled(),
                new ArrayList<>(user.getAuthorities()),
                getUserIdByUsername(key)));
    }

    private void writeXmlDocument(Document doc) throws Exception {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(javax.xml.transform.OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
        transformer.transform(new DOMSource(doc), new StreamResult(xmlFilePath.toFile()));
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        String key = username.toLowerCase(Locale.ROOT);
        log.info("Attempting to load user: {}. Available users: {}", username, users.keySet());
        UserDetails user = users.get(key);
        if (user == null) {
            log.warn("User not found: {}. Available users: {}", username, users.keySet());
            throw new UsernameNotFoundException("User not found: " + username);
        }
        if (!CamundaBcryptPasswordEncryptor.isBcryptHash(user.getPassword())) {
            String resolved = resolvePassword(username, user.getPassword());
            if (CamundaBcryptPasswordEncryptor.isBcryptHash(resolved)) {
                XmlUserPrincipal repaired = new XmlUserPrincipal(
                        user.getUsername(),
                        resolved,
                        user.isEnabled(),
                        new ArrayList<>(user.getAuthorities()),
                        getUserIdByUsername(key));
                users.put(key, repaired);
                persistPasswordHashToXml(username, resolved);
                log.info("Using database password hash for '{}'", username);
                return repaired;
            }
            log.warn("User '{}' has no valid BCrypt password in XML or database", username);
        }
        log.info("User found: {}", username);
        return user;
    }

    private String lookupDatabasePasswordHash(String username) {
        return userRepository.findByLoginIgnoreCaseOrEmailIgnoreCase(username, username)
                .map(com.mts.online_shop.model.User::getPasswordHash)
                .orElse(null);
    }

    public String lookupTemplatePasswordHashForUser(String username) {
        return lookupTemplatePasswordHash(username);
    }

    private String lookupTemplatePasswordHash(String username) {
        try {
            ClassPathResource resource = new ClassPathResource(UsersXmlLocation.XML_FILE_NAME);
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(resource.getInputStream());
            NodeList userNodes = doc.getElementsByTagName("user");
            String key = username.trim().toLowerCase(Locale.ROOT);
            for (int i = 0; i < userNodes.getLength(); i++) {
                Element userElement = (Element) userNodes.item(i);
                if (key.equals(userElement.getAttribute("username").toLowerCase(Locale.ROOT))) {
                    return userElement.getAttribute("password");
                }
            }
        } catch (Exception e) {
            log.debug("Could not read template password for '{}'", username, e);
        }
        return null;
    }
}
