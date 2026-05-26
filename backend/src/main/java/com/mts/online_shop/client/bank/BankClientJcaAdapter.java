package com.mts.online_shop.client.bank;

import com.mts.online_shop.bank.jca.BankInteractionSpec;
import com.mts.online_shop.bank.jca.SimpleMappedRecord;
import com.mts.online_shop.exception.InvalidPaymentDataException;
import com.mts.online_shop.model.PaymentRequest;
import jakarta.resource.ResourceException;
import jakarta.resource.cci.Connection;
import jakarta.resource.cci.ConnectionFactory;
import jakarta.resource.cci.Interaction;
import jakarta.resource.cci.MappedRecord;
import jakarta.resource.cci.RecordFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class BankClientJcaAdapter implements BankClient {

    private static final Logger log = LoggerFactory.getLogger(BankClientJcaAdapter.class);

    private final ConnectionFactory connectionFactory;

    public BankClientJcaAdapter(@Qualifier("bankJcaConnectionFactory") ConnectionFactory bankJcaConnectionFactory) {
        this.connectionFactory = bankJcaConnectionFactory;
    }

    @Override
    public boolean doPayment(PaymentRequest paymentRequest, BigDecimal amount) {
        if (paymentRequest == null) {
            throw new InvalidPaymentDataException("Payment data is required");
        }
        if (amount == null || amount.signum() < 0) {
            throw new InvalidPaymentDataException("Total price must be non-negative");
        }

        try {
            RecordFactory rf = connectionFactory.getRecordFactory();
            MappedRecord<String, Object> input = rf.createMappedRecord("paymentInput");
            input.put("cardNumber", paymentRequest.getCardNumber());
            input.put("cvv", paymentRequest.getCvv());
            input.put("expiresAt", paymentRequest.getExpiresAt());
            input.put("amount", amount.floatValue());

            MappedRecord<String, Object> output = new SimpleMappedRecord("paymentOutput");

            Connection connection = connectionFactory.getConnection();
            try {
                Interaction interaction = connection.createInteraction();
                boolean ok = interaction.execute(
                        new BankInteractionSpec(BankInteractionSpec.FUNCTION_PAYMENT),
                        input,
                        output
                );
                Boolean approved = output.get("approved") instanceof Boolean b ? b : Boolean.FALSE;
                String message = output.get("message") != null ? output.get("message").toString() : "";
                log.info("Bank JCA payment: ok={}, approved={}, message={}", ok, approved, message);
                if (!ok || !approved) {
                    throw new InvalidPaymentDataException(message.isBlank() ? "Payment failed" : message);
                }
                return true;
            } finally {
                try {
                    connection.close();
                } catch (ResourceException closeEx) {
                    log.debug("Bank JCA connection close: {}", closeEx.getMessage());
                }
            }
        } catch (InvalidPaymentDataException e) {
            throw e;
        } catch (ResourceException e) {
            log.error("Bank JCA resource error", e);
            throw new InvalidPaymentDataException("Bank service error: " + e.getMessage());
        } catch (Exception e) {
            log.error("Bank JCA unexpected error", e);
            throw new InvalidPaymentDataException("Bank service unavailable: " + e.getMessage());
        }
    }

    @Override
    public boolean refundPayment(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new InvalidPaymentDataException("Refund amount must be positive");
        }
        try {
            RecordFactory rf = connectionFactory.getRecordFactory();
            MappedRecord<String, Object> input = rf.createMappedRecord("refundInput");
            input.put("amount", amount.floatValue());
            MappedRecord<String, Object> output = new SimpleMappedRecord("refundOutput");
            Connection connection = connectionFactory.getConnection();
            try {
                Interaction interaction = connection.createInteraction();
                interaction.execute(
                        new BankInteractionSpec(BankInteractionSpec.FUNCTION_REFUND),
                        input,
                        output
                );
                return Boolean.TRUE.equals(output.get("approved"));
            } finally {
                try {
                    connection.close();
                } catch (ResourceException closeEx) {
                    log.debug("Bank JCA connection close: {}", closeEx.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("Bank JCA refund error", e);
            throw new InvalidPaymentDataException("Refund failed: " + e.getMessage());
        }
    }
}
