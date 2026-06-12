package com.mts.online_shop.camunda;

import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.repository.Deployment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Проверка, что Camunda Forms (.form) попали в тот же auto-deployment, что и BPMN.
 * Иначе Tasklist показывает «Form failure: Not Found» при formRefBinding=deployment.
 */
@Component
public class CamundaDeploymentVerifier {

    private static final Logger log = LoggerFactory.getLogger(CamundaDeploymentVerifier.class);

    private final RepositoryService repositoryService;

    public CamundaDeploymentVerifier(RepositoryService repositoryService) {
        this.repositoryService = repositoryService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void verifyFormsDeployed() {
        Deployment deployment = repositoryService.createDeploymentQuery()
                .orderByDeploymentTime()
                .desc()
                .listPage(0, 1)
                .stream()
                .findFirst()
                .orElse(null);
        if (deployment == null) {
            log.warn("No Camunda deployments found — BPMN/forms were not auto-deployed");
            return;
        }

        List<String> resourceNames = repositoryService.getDeploymentResourceNames(deployment.getId());
        long formCount = resourceNames.stream().filter(name -> name.endsWith(".form")).count();
        long bpmnCount = resourceNames.stream().filter(name -> name.endsWith(".bpmn")).count();

        log.info("Camunda deployment '{}' (id={}): {} BPMN, {} forms, resources={}",
                deployment.getName(), deployment.getId(), bpmnCount, formCount, resourceNames);

        if (bpmnCount > 0 && formCount == 0) {
            log.error("BPMN deployed without .form files — Tasklist forms will fail with 'Not Found'. "
                    + "Check camunda.bpm.deployment-resource-pattern and redeploy WAR.");
        } else if (formCount > 0 && formCount < 7) {
            log.warn("Expected 7 Camunda forms, found {}", formCount);
        }
        if (bpmnCount != 5) {
            log.warn("Expected 5 BPMN processes, found {}", bpmnCount);
        }
    }
}
