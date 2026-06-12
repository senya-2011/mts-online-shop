package com.mts.online_shop.service;

import com.mts.online_shop.client.bank.BankClientProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Периодическая проверка доступности банковского EIS (ЛР3), вне BPMN.
 */
@Component
public class BankReachabilityScheduler {

    private static final Logger log = LoggerFactory.getLogger(BankReachabilityScheduler.class);

    private final RestClient restClient;

    public BankReachabilityScheduler(BankClientProperties bankClientProperties) {
        this.restClient = RestClient.builder()
                .baseUrl(bankClientProperties.getBaseUrl())
                .build();
    }

    @Scheduled(fixedRateString = "${app.bank.health-check-ms:300000}")
    public void checkBankReachability() {
        try {
            restClient.get()
                    .uri("/api/cards")
                    .retrieve()
                    .toBodilessEntity();
            log.info("Bank EIS is reachable at /api/cards");
        } catch (Exception e) {
            log.warn("Bank EIS health check failed: {}", e.getMessage());
        }
    }
}
