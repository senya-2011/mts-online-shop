package com.mts.online_shop.camunda;

import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.repository.Deployment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Убирает старые auto-deployment'ы Camunda, чтобы в Cockpit не копились десятки версий
 * и удалённые процессы (mts-order-purchase, mts-auth-login и т.д.).
 */
@Component
public class CamundaDeploymentCleaner {

    private static final Logger log = LoggerFactory.getLogger(CamundaDeploymentCleaner.class);

    private final RepositoryService repositoryService;
    private final RuntimeService runtimeService;

    public CamundaDeploymentCleaner(RepositoryService repositoryService, RuntimeService runtimeService) {
        this.repositoryService = repositoryService;
        this.runtimeService = runtimeService;
    }

    @Order(0)
    @EventListener(ApplicationReadyEvent.class)
    public void removeStaleDeployments() {
        List<Deployment> deployments = repositoryService.createDeploymentQuery()
                .orderByDeploymentTime()
                .desc()
                .list();
        if (deployments.size() <= 1) {
            return;
        }

        Deployment current = deployments.get(0);
        int removed = 0;
        for (int i = 1; i < deployments.size(); i++) {
            Deployment stale = deployments.get(i);
            long running = runtimeService.createProcessInstanceQuery()
                    .deploymentId(stale.getId())
                    .active()
                    .count();
            if (running > 0) {
                log.info("Skip Camunda deployment id={}: {} active process instance(s)", stale.getId(), running);
                continue;
            }
            try {
                repositoryService.deleteDeployment(stale.getId(), true);
                removed++;
                log.info("Removed stale Camunda deployment id={} name={}", stale.getId(), stale.getName());
            } catch (Exception e) {
                log.warn("Could not remove Camunda deployment id={}: {}", stale.getId(), e.getMessage());
            }
        }
        log.info("Camunda deployments cleaned: kept id={}, removed {}", current.getId(), removed);
    }
}
