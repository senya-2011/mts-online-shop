package com.mts.online_shop.client.bitrix;

import com.mts.online_shop.bank.jca.SimpleMappedRecord;
import com.mts.online_shop.bitrix.jca.BitrixInteractionSpec;
import jakarta.resource.ResourceException;
import jakarta.resource.cci.Connection;
import jakarta.resource.cci.ConnectionFactory;
import jakarta.resource.cci.Interaction;
import jakarta.resource.cci.MappedRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@Component
@ConditionalOnBean(name = "bitrixJcaConnectionFactory")
public class BitrixEisClientJcaAdapter {

    private static final Logger log = LoggerFactory.getLogger(BitrixEisClientJcaAdapter.class);
    private static final DateTimeFormatter TS_FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    private final BitrixEisProperties properties;
    private final ConnectionFactory connectionFactory;

    public BitrixEisClientJcaAdapter(
            BitrixEisProperties properties,
            @Qualifier("bitrixJcaConnectionFactory") ConnectionFactory connectionFactory
    ) {
        this.properties = properties;
        this.connectionFactory = connectionFactory;
    }

    public void publishOrderPaid(Long orderId, Long userId, BigDecimal totalPrice) {
        if (!properties.isEnabled()) {
            return;
        }
        String title = "Оплата заказа #" + orderId;
        String message = "Заказ успешно оплачен.\n"
                + "orderId: " + orderId + "\n"
                + "userId: " + userId + "\n"
                + "amount: " + totalPrice + "\n"
                + "timestamp: " + TS_FORMATTER.format(Instant.now().atOffset(ZoneOffset.UTC));
        sendBlogPost(title, message);
    }

    private void sendBlogPost(String title, String message) {
        Connection connection = null;
        try {
            connection = connectionFactory.getConnection();
            Interaction interaction = connection.createInteraction();
            MappedRecord<String, Object> input = new SimpleMappedRecord("bitrixBlogPostInput");
            input.put("postTitle", title);
            input.put("postMessage", message);

            MappedRecord<String, Object> output = new SimpleMappedRecord("bitrixBlogPostOutput");
            boolean ok = interaction.execute(
                    new BitrixInteractionSpec(BitrixInteractionSpec.FUNCTION_BLOGPOST_ADD),
                    input,
                    output
            );
            if (!ok) {
                log.warn("Bitrix blogpost add returned not-ok: {}", output);
                return;
            }
            log.info("Bitrix blogpost add success: {}", output.get("result"));
        } catch (Exception e) {
            // Best-effort integration: do not break order payment.
            log.warn("Bitrix EIS publish skipped due to error: {}", e.getMessage());
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (ResourceException closeEx) {
                    log.debug("Bitrix JCA connection close: {}", closeEx.getMessage());
                }
            }
        }
    }
}
