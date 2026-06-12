package com.mts.online_shop.camunda;

import org.camunda.bpm.engine.AuthorizationService;
import org.camunda.bpm.engine.authorization.Authorization;
import org.camunda.bpm.engine.authorization.Permissions;
import org.camunda.bpm.engine.authorization.Resources;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Разграничение старта процессов в Camunda Tasklist: {@code user} — только USER-процессы,
 * {@code admin} — все процессы (USER + ADMIN + SERVER); при синхронизации identity
 * администратор дополнительно входит в группу {@code user} для user task в Tasklist.
 */
@Component
public class CamundaProcessAuthorizationConfigurer {

    private static final Logger log = LoggerFactory.getLogger(CamundaProcessAuthorizationConfigurer.class);

    static final String GROUP_USER = "user";
    static final String GROUP_ADMIN = "admin";
    static final String GROUP_CAMUNDA_ADMIN = "camunda-admin";

    private static final List<String> USER_PROCESS_KEYS = List.of(
            "user-lk-order",
            "user-order-cancel"
    );

    private static final List<String> ALL_PROCESS_KEYS = List.of(
            "user-lk-order",
            "user-order-cancel",
            "admin-product-create",
            "admin-product-update",
            "server-order-processing"
    );

    private final AuthorizationService authorizationService;

    public CamundaProcessAuthorizationConfigurer(AuthorizationService authorizationService) {
        this.authorizationService = authorizationService;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Order(300)
    public void configureAuthorizations() {
        USER_PROCESS_KEYS.forEach(key -> grantProcessDefinition(GROUP_USER, key));
        ALL_PROCESS_KEYS.forEach(key -> grantProcessDefinition(GROUP_ADMIN, key));
        grantTaskAndInstancePermissions(GROUP_USER);
        grantTaskAndInstancePermissions(GROUP_ADMIN);
        grantWebappAccessForUserGroup();
        grantWebappAccessForAdminGroup();
        grantWebappAccessForCamundaAdminGroup();
        ALL_PROCESS_KEYS.forEach(key -> grantProcessDefinition(GROUP_CAMUNDA_ADMIN, key));
        grantTaskAndInstancePermissions(GROUP_CAMUNDA_ADMIN);
        grantAuthorizationAdmin(GROUP_CAMUNDA_ADMIN);
        grantAuthorizationAdmin(GROUP_ADMIN);
        log.info("Camunda authorizations configured: user -> {}, admin/camunda-admin -> all {}",
                USER_PROCESS_KEYS, ALL_PROCESS_KEYS);
    }

    private void grantProcessDefinition(String groupId, String processDefinitionKey) {
        replaceGrant(groupId, Resources.PROCESS_DEFINITION, processDefinitionKey,
                Permissions.READ,
                Permissions.CREATE_INSTANCE,
                Permissions.READ_INSTANCE,
                Permissions.UPDATE_INSTANCE);
    }

    private void grantTaskAndInstancePermissions(String groupId) {
        replaceGrant(groupId, Resources.TASK, "*",
                Permissions.READ,
                Permissions.UPDATE,
                Permissions.CREATE);
        replaceGrant(groupId, Resources.PROCESS_INSTANCE, "*",
                Permissions.READ,
                Permissions.UPDATE,
                Permissions.CREATE);
        // Tasklist filters («All tasks») не видны без READ на FILTER при authorization.enabled=true
        replaceGrant(groupId, Resources.FILTER, "*",
                Permissions.READ,
                Permissions.UPDATE,
                Permissions.CREATE);
    }

    private void grantAuthorizationAdmin(String groupId) {
        replaceGrant(groupId, Resources.AUTHORIZATION, "*",
                Permissions.READ,
                Permissions.CREATE,
                Permissions.UPDATE,
                Permissions.DELETE);
    }

    private void grantWebappAccessForUserGroup() {
        // Обычный пользователь: только Welcome и Tasklist (без Cockpit и Admin)
        grantWebappApps(GROUP_USER, List.of("welcome", "tasklist"));
        revokeWildcardWebappAccess(GROUP_USER);
    }

    private void grantWebappAccessForAdminGroup() {
        grantWebappApps(GROUP_ADMIN, List.of("welcome", "tasklist", "cockpit", "admin"));
        revokeWildcardWebappAccess(GROUP_ADMIN);
    }

    private void grantWebappAccessForCamundaAdminGroup() {
        grantWebappApps(GROUP_CAMUNDA_ADMIN, List.of("welcome", "tasklist", "cockpit", "admin"));
        replaceGrant(GROUP_CAMUNDA_ADMIN, Resources.APPLICATION, "*", Permissions.ACCESS);
    }

    private void grantWebappApps(String groupId, List<String> appIds) {
        for (String app : appIds) {
            replaceGrant(groupId, Resources.APPLICATION, app, Permissions.ACCESS);
        }
        for (String app : List.of("welcome", "tasklist", "cockpit", "admin")) {
            if (!appIds.contains(app)) {
                revokeGroupWebappAccess(groupId, app);
            }
        }
    }

    private void revokeWildcardWebappAccess(String groupId) {
        authorizationService.createAuthorizationQuery()
                .authorizationType(Authorization.AUTH_TYPE_GRANT)
                .groupIdIn(groupId)
                .resourceType(Resources.APPLICATION)
                .resourceId("*")
                .list()
                .forEach(existing -> authorizationService.deleteAuthorization(existing.getId()));
    }

    private void revokeGroupWebappAccess(String groupId, String appId) {
        authorizationService.createAuthorizationQuery()
                .authorizationType(Authorization.AUTH_TYPE_GRANT)
                .groupIdIn(groupId)
                .resourceType(Resources.APPLICATION)
                .resourceId(appId)
                .list()
                .forEach(existing -> authorizationService.deleteAuthorization(existing.getId()));
    }

    private void replaceGrant(String groupId, Resources resource, String resourceId, Permissions... permissions) {
        authorizationService.createAuthorizationQuery()
                .authorizationType(Authorization.AUTH_TYPE_GRANT)
                .groupIdIn(groupId)
                .resourceType(resource)
                .resourceId(resourceId)
                .list()
                .forEach(existing -> authorizationService.deleteAuthorization(existing.getId()));

        Authorization authorization = authorizationService.createNewAuthorization(Authorization.AUTH_TYPE_GRANT);
        authorization.setGroupId(groupId);
        authorization.setResource(resource);
        authorization.setResourceId(resourceId);
        for (Permissions permission : permissions) {
            authorization.addPermission(permission);
        }
        authorizationService.saveAuthorization(authorization);
    }
}
