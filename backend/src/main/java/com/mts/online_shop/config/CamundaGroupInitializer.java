package com.mts.online_shop.config;

import jakarta.annotation.PostConstruct;
import org.camunda.bpm.engine.IdentityService;
import org.camunda.bpm.engine.identity.Group;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class CamundaGroupInitializer {

    private static final Logger log = LoggerFactory.getLogger(CamundaGroupInitializer.class);

    private final IdentityService identityService;

    public CamundaGroupInitializer(IdentityService identityService) {
        this.identityService = identityService;
    }

    @PostConstruct
    public void initGroups() {
        List<String> groups = Arrays.asList("USER", "ADMIN");
        for (String g : groups) {
            try {
                if (identityService.createGroupQuery().groupId(g).count() == 0) {
                    Group group = identityService.newGroup(g);
                    group.setName(g);
                    identityService.saveGroup(group);
                    log.info("Created Camunda group: {}", g);
                }
            } catch (Exception e) {
                log.warn("Failed to ensure Camunda group {}: {}", g, e.getMessage());
            }
        }
    }
}
