package com.mts.online_shop.bitrix.jca;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mts.online_shop.bank.jca.SimpleMappedRecord;
import jakarta.resource.ResourceException;
import jakarta.resource.cci.Connection;
import jakarta.resource.cci.Interaction;
import jakarta.resource.cci.InteractionSpec;
import jakarta.resource.cci.MappedRecord;
import jakarta.resource.cci.Record;
import jakarta.resource.cci.ResourceWarning;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

public final class BitrixCciInteraction implements Interaction {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final BitrixManagedConnection mc;
    private final Connection connection;
    private ResourceWarning warning;

    BitrixCciInteraction(BitrixManagedConnection mc, Connection connection) {
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
        if (!(ispec instanceof BitrixInteractionSpec spec)) {
            throw new ResourceException("Unsupported InteractionSpec: " + ispec);
        }
        if (!(input instanceof MappedRecord<?, ?> inMap) || !(output instanceof MappedRecord<?, ?> outMap)) {
            throw new ResourceException("Input and output must be MappedRecord");
        }

        if (!BitrixInteractionSpec.FUNCTION_BLOGPOST_ADD.equals(spec.getFunctionName())) {
            throw new ResourceException("Unsupported function: " + spec.getFunctionName());
        }

        @SuppressWarnings("unchecked")
        MappedRecord<String, Object> inTyped = (MappedRecord<String, Object>) inMap;
        @SuppressWarnings("unchecked")
        MappedRecord<String, Object> outTyped = (MappedRecord<String, Object>) outMap;

        String title = valueAsString(inTyped.get("postTitle"));
        String message = valueAsString(inTyped.get("postMessage"));
        if (title == null || title.isBlank() || message == null || message.isBlank()) {
            throw new ResourceException("Missing postTitle/postMessage");
        }

        String base = mc.getMcf().getWebhookBaseUrl();
        if (base == null || base.isBlank()) {
            throw new ResourceException("Bitrix webhook base URL is empty");
        }
        String baseNormalized = base.replaceAll("/+$", "");
        String endpoint = baseNormalized + "/log.blogpost.add.json";

        String payload = "POST_TITLE=" + urlEncode(title) + "&POST_MESSAGE=" + urlEncode(message);

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .timeout(Duration.ofSeconds(20))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = mc.getHttpClient().send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            JsonNode json = MAPPER.readTree(response.body());

            if (response.statusCode() >= 400 || json.hasNonNull("error")) {
                String error = json.hasNonNull("error_description")
                        ? json.get("error_description").asText()
                        : json.path("error").asText("Bitrix error");
                outTyped.put("success", false);
                outTyped.put("message", error);
                outTyped.put("statusCode", response.statusCode());
                return false;
            }

            outTyped.put("success", true);
            outTyped.put("statusCode", response.statusCode());
            outTyped.put("result", json.path("result").asText(""));
            outTyped.put("message", "ok");
            return true;
        } catch (Exception e) {
            throw new ResourceException("Bitrix HTTP call failed: " + e.getMessage(), e);
        }
    }

    @Override
    public Record execute(InteractionSpec ispec, Record input) throws ResourceException {
        SimpleMappedRecord output = new SimpleMappedRecord("output");
        execute(ispec, input, output);
        return output;
    }

    private static String valueAsString(Object val) {
        return val == null ? null : val.toString();
    }

    private static String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
