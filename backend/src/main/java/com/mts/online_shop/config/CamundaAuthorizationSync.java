package com.mts.online_shop.config;

import org.camunda.bpm.engine.AuthorizationService;
import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.authorization.Authorization;
import org.camunda.bpm.engine.authorization.Permissions;
import org.camunda.bpm.engine.authorization.Resources;
import org.camunda.bpm.engine.repository.ProcessDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class CamundaAuthorizationSync {

    private static final Logger log = LoggerFactory.getLogger(CamundaAuthorizationSync.class);

    private final ProcessEngine processEngine;
    private final RepositoryService repositoryService;

    public CamundaAuthorizationSync(ProcessEngine processEngine) {
        this.processEngine = processEngine;
        this.repositoryService = processEngine.getRepositoryService();
    }

    @EventListener(ApplicationReadyEvent.class)
    public void syncAuthorizations() {
        try {
            AuthorizationService authorizationService = processEngine.getAuthorizationService();
            List<ProcessDefinition> defs = repositoryService.createProcessDefinitionQuery().list();

            // mapping: processKey -> allowed groups to start
            Map<String, String[]> allowed = new HashMap<>();
            allowed.put("order_process", new String[]{"CLIENT","OPERATOR","ADMIN"});
            allowed.put("cancel_order", new String[]{"OPERATOR","ADMIN"});
            allowed.put("periodic_order_cleanup", new String[]{"ADMIN"});

            for (ProcessDefinition pd : defs) {
                String key = pd.getKey();
                String[] groups = allowed.get(key);
                if (groups == null) continue;
                for (String g : groups) {
                    Authorization a = authorizationService.createNewAuthorization(Authorization.AUTH_TYPE_GRANT);
                    a.setResource(Resources.PROCESS_DEFINITION);
                    a.setResourceId(key);
                    a.addPermission(Permissions.CREATE_INSTANCE);
                    a.setGroupId(g);
                    authorizationService.saveAuthorization(a);
                    log.info("Granted CREATE_INSTANCE on process {} to group {}", key, g);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to sync Camunda authorizations: {}", e.getMessage());
        }
    }
}
