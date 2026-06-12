package com.mts.online_shop.camunda;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(100)
public class CamundaIdentitySynchronizer implements ApplicationRunner {

    private final CamundaIdentityService camundaIdentityService;

    public CamundaIdentitySynchronizer(CamundaIdentityService camundaIdentityService) {
        this.camundaIdentityService = camundaIdentityService;
    }

    @Override
    public void run(ApplicationArguments args) {
        camundaIdentityService.syncAllShopUsers();
    }
}
