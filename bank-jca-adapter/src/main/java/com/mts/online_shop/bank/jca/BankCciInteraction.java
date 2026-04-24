package com.mts.online_shop.bank.jca;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.resource.ResourceException;
import jakarta.resource.cci.Connection;
import jakarta.resource.cci.Interaction;
import jakarta.resource.cci.InteractionSpec;
import jakarta.resource.cci.MappedRecord;
import jakarta.resource.cci.Record;
import jakarta.resource.cci.ResourceWarning;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

public final class BankCciInteraction implements Interaction {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final BankManagedConnection mc;
    private final Connection connection;
    private ResourceWarning warning;

    BankCciInteraction(BankManagedConnection mc, Connection connection) {
        this.mc = mc;
        this.connection = connection;
    }

    @Override
    public Connection getConnection() {
        return connection;
    }

    @Override
    public void clearWarnings() throws ResourceException {
        warning = null;
    }

    @Override
    public ResourceWarning getWarnings() throws ResourceException {
        return warning;
    }

    @Override
    public void close() throws ResourceException {
        // no-op: connection owns lifecycle
    }

    @Override
    public boolean execute(InteractionSpec ispec, Record input, Record output) throws ResourceException {
        if (!(ispec instanceof BankInteractionSpec spec)) {
            throw new ResourceException("Unsupported InteractionSpec: " + ispec);
        }
        if (!(input instanceof MappedRecord<?, ?> inMap) || !(output instanceof MappedRecord<?, ?> outMap)) {
            throw new ResourceException("Input and output must be MappedRecord");
        }
        @SuppressWarnings("unchecked")
        MappedRecord<String, Object> inTyped = (MappedRecord<String, Object>) input;
        @SuppressWarnings("unchecked")
        MappedRecord<String, Object> outTyped = (MappedRecord<String, Object>) output;

        if (BankInteractionSpec.FUNCTION_REFUND.equals(spec.getFunctionName())) {
            outTyped.put("approved", true);
            outTyped.put("message", "Refund simulated (no bank endpoint)");
            return true;
        }

        if (!BankInteractionSpec.FUNCTION_PAYMENT.equals(spec.getFunctionName())) {
            throw new ResourceException("Unsupported function: " + spec.getFunctionName());
        }

        Map<String, Object> in = readMapped(inTyped);
        String cardNumber = stringVal(in.get("cardNumber"));
        String cvv = stringVal(in.get("cvv"));
        String expiresAt = stringVal(in.get("expiresAt"));
        Object amountObj = in.get("amount");
        if (cardNumber == null || cvv == null || expiresAt == null || amountObj == null) {
            throw new ResourceException("Missing cardNumber, cvv, expiresAt or amount");
        }

        float amount;
        if (amountObj instanceof Number n) {
            amount = n.floatValue();
        } else {
            amount = Float.parseFloat(amountObj.toString());
        }

        ObjectNode body = MAPPER.createObjectNode();
        body.put("cardNumber", cardNumber);
        body.put("cvv", cvv);
        body.put("expiresAt", expiresAt);
        body.put("amount", amount);

        String base = mc.getMcf().getBankBaseUrl().replaceAll("/$", "");
        String url = base + "/api/cards/payments";

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(30))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(MAPPER.writeValueAsString(body), StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = mc.getHttpClient().send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            int status = response.statusCode();
            JsonNode root = MAPPER.readTree(response.body());
            Boolean approved = root.has("approved") ? root.get("approved").asBoolean() : null;
            String message = root.has("message") && !root.get("message").isNull() ? root.get("message").asText() : "";
            if (root.has("remainingBalance") && !root.get("remainingBalance").isNull()) {
                outTyped.put("remainingBalance", root.get("remainingBalance").floatValue());
            }

            outTyped.put("approved", approved != null && approved);
            outTyped.put("message", message);

            if (status >= 400) {
                return Boolean.FALSE.equals(approved);
            }
            return Boolean.TRUE.equals(approved);
        } catch (Exception e) {
            throw new ResourceException("Bank HTTP call failed: " + e.getMessage(), e);
        }
    }

    @Override
    public Record execute(InteractionSpec ispec, Record input) throws ResourceException {
        SimpleMappedRecord output = new SimpleMappedRecord("output");
        execute(ispec, input, output);
        return output;
    }

    private static Map<String, Object> readMapped(MappedRecord<String, Object> inMap) throws ResourceException {
        Map<String, Object> m = new LinkedHashMap<>();
        for (String k : inMap.keySet()) {
            m.put(k, inMap.get(k));
        }
        return m;
    }

    private static String stringVal(Object o) {
        return o == null ? null : o.toString();
    }
}
