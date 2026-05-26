package com.mts.online_shop.service;

import com.mts.online_shop.client.bank.BankClientProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class BankReachabilityScheduler {

    private static final Logger log = LoggerFactory.getLogger(BankReachabilityScheduler.class);

    private final BankClientProperties bankClientProperties;
    private final RestTemplate restTemplate = new RestTemplate();

    public BankReachabilityScheduler(BankClientProperties bankClientProperties) {
        this.bankClientProperties = bankClientProperties;
    }

    @Scheduled(fixedRateString = "${app.bank.health-check-ms:300000}")
    public void pingBank() {
        String base = bankClientProperties.getBaseUrl().replaceAll("/$", "");
        String url = base + "/api/cards";
        try {
            restTemplate.getForEntity(url, String.class);
            log.debug("Bank health check OK: {}", url);
        } catch (Exception e) {
            log.warn("Bank health check failed for {}: {}", url, e.getMessage());
        }
    }
}
