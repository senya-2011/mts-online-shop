package com.mts.online_shop.client.bank;

import com.mts.online_shop.exception.InvalidPaymentDataException;
import com.mts.online_shop.model.BankPaymentRequest;
import com.mts.online_shop.model.BankPaymentResponse;
import com.mts.online_shop.model.PaymentRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;

@Component
public class BankApiClient implements BankClient {

    private static final Logger log = LoggerFactory.getLogger(BankApiClient.class);
    private static final String PAYMENTS_PATH = "/api/cards/payments";

    private final RestClient restClient;

    public BankApiClient(BankClientProperties properties) {
        String baseUrl = properties.getBaseUrl() != null ? properties.getBaseUrl().replaceAll("/$", "") : "http://localhost:8081";
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
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
            BankPaymentResponse response = restClient.post()
                    .uri(PAYMENTS_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(BankPaymentResponse.class);

            if (response == null) {
                log.warn("Bank returned empty response");
                throw new InvalidPaymentDataException("Payment failed");
            }
            boolean approved = Boolean.TRUE.equals(response.getApproved());
            log.debug("Bank payment approved={} message={}", approved, response.getMessage());
            if (!approved && response.getMessage() != null) {
                throw new InvalidPaymentDataException(response.getMessage());
            }
            return approved;
        } catch (InvalidPaymentDataException e) {
            throw e;
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            String message = "Payment failed";
            if (e.getResponseBodyAsString() != null && !e.getResponseBodyAsString().isBlank()) {
                try {
                    BankPaymentResponse err = e.getResponseBodyAs(BankPaymentResponse.class);
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
}
