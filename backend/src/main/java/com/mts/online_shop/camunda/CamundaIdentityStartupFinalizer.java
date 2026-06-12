package com.mts.online_shop.camunda;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * После authorizations: сброс lock и пересинхронизация demo/admin/user с известными паролями.
 */
@Component
public class CamundaIdentityStartupFinalizer {

    private final CamundaIdentityService camundaIdentityService;

    public CamundaIdentityStartupFinalizer(CamundaIdentityService camundaIdentityService) {
        this.camundaIdentityService = camundaIdentityService;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Order(350)
    public void finalizeIdentity() {
        camundaIdentityService.unlockAllIdentityUsers();
        camundaIdentityService.syncDefaultShopAccounts();
        camundaIdentityService.syncBuiltInAdminUser();
        camundaIdentityService.reconcileAllUserWebappAccess();
        camundaIdentityService.unlockAllIdentityUsers();
    }
}
