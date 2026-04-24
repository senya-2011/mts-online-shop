package com.mts.online_shop.client.bank;

import com.mts.online_shop.exception.InvalidPaymentDataException;
import com.mts.online_shop.model.BankPaymentRequest;
import com.mts.online_shop.model.BankPaymentResponse;
import com.mts.online_shop.model.PaymentRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class BankApiClient implements BankClient {

    private static final Logger log = LoggerFactory.getLogger(BankApiClient.class);
    private static final String PAYMENTS_PATH = "/api/cards/payments";

    private final RestTemplate restTemplate;

    public BankApiClient(BankClientProperties properties) {
        String baseUrl = properties.getBaseUrl() != null ? properties.getBaseUrl().replaceAll("/$", "") : "http://localhost:8081";
        this.restTemplate = new RestTemplate();
        log.info("Bank client baseUrl={}", baseUrl);
    }

    @Override
    public boolean doPayment(PaymentRequest paymentRequest, BigDecimal amount) {
        if (paymentRequest == null) {
            throw new InvalidPaymentDataException("Payment data is required");
        }
        if (amount == null || amount.signum() < 0) {
            throw new InvalidPaymentDataException("Total price must be non-negative");
        }

        BankPaymentRequest body = new BankPaymentRequest(
                paymentRequest.getCardNumber(),
                paymentRequest.getCvv(),
                paymentRequest.getExpiresAt(),
                amount.floatValue()
        );

        try {
            String url = "http://localhost:8081" + PAYMENTS_PATH;
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<BankPaymentRequest> entity = new HttpEntity<>(body, headers);
            
            BankPaymentResponse response = restTemplate.postForObject(url, entity, BankPaymentResponse.class);

            if (response == null) {
                log.warn("Bank returned empty response");
                throw new InvalidPaymentDataException("Payment failed");
            }
            boolean approved = Boolean.TRUE.equals(response.getApproved());
            log.info("Bank response: approved={}, message={}", 
                approved, 
                response.getMessage());
            if (!approved && response.getMessage() != null) {
                log.error("Bank payment not approved: {}", response.getMessage());
                throw new InvalidPaymentDataException(response.getMessage());
            }
            return approved;
        } catch (InvalidPaymentDataException e) {
            throw e;
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            String message = "Payment failed";
            if (e.getResponseBodyAsString() != null && !e.getResponseBodyAsString().isBlank()) {
                try {
                    ObjectMapper mapper = new ObjectMapper();
                    BankPaymentResponse err = mapper.readValue(e.getResponseBodyAsString(), BankPaymentResponse.class);
                    if (err != null && err.getMessage() != null) {
                        message = err.getMessage();
                    }
                } catch (Exception ignored) {
                }
            }
            log.warn("Bank returned {}: {}", e.getStatusCode(), message);
            throw new InvalidPaymentDataException(message);
        } catch (Exception e) {
            log.error("Bank request failed", e);
            throw new InvalidPaymentDataException("Bank service unavailable: " + e.getMessage());
        }
    }

    @Override
    public boolean refundPayment(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new InvalidPaymentDataException("Refund amount must be positive");
        }

        try {
            // Имитируем возврат через банк
            log.info("Processing refund of amount: {}", amount);
            
            // В реальном приложении здесь был бы запрос к банковскому API
            // Для демонстрации просто возвращаем true
            boolean refundProcessed = true;
            
            log.debug("Refund processed successfully for amount: {}", amount);
            return refundProcessed;
        } catch (Exception e) {
            log.error("Refund failed", e);
            throw new InvalidPaymentDataException("Refund failed: " + e.getMessage());
        }
    }
}
