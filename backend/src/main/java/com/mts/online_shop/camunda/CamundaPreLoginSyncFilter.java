package com.mts.online_shop.camunda;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

/**
 * Перед JSON-логином в Camunda Tasklist синхронизирует учётку из БД/users.xml.
 * Обрабатывает только POST .../auth/user/{engine}/login (не /login/admin).
 */
public class CamundaPreLoginSyncFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(CamundaPreLoginSyncFilter.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final int MAX_BODY_BYTES = 16 * 1024;

    private final CamundaIdentityService camundaIdentityService;

    public CamundaPreLoginSyncFilter(CamundaIdentityService camundaIdentityService) {
        this.camundaIdentityService = camundaIdentityService;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (!(request instanceof HttpServletRequest httpRequest) || !isCamundaCredentialLoginPost(httpRequest)) {
            chain.doFilter(request, response);
            return;
        }
        if (isBodyTooLarge(httpRequest)) {
            log.warn("Skip Camunda pre-login sync: request body too large for {}", httpRequest.getRequestURI());
            chain.doFilter(request, response);
            return;
        }
        CachedBodyHttpServletRequest cachedRequest = new CachedBodyHttpServletRequest(httpRequest);
        try {
            LoginPayload payload = parseLoginPayload(cachedRequest.getBody(), cachedRequest.getContentType());
            if (payload != null
                    && payload.username() != null
                    && !payload.username().isBlank()
                    && payload.password() != null) {
                camundaIdentityService.syncOnLogin(payload.username(), payload.password());
                log.debug("Camunda pre-login identity sync completed for '{}'", payload.username());
            }
        } catch (Exception ex) {
            log.warn("Camunda pre-login identity sync failed: {}", ex.getMessage(), ex);
        }
        chain.doFilter(cachedRequest, response);
    }

    /** Только POST .../user/{engine}/login — без /login/admin и прочих под-путей. */
    private static boolean isCamundaCredentialLoginPost(HttpServletRequest request) {
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            return false;
        }
        String uri = request.getRequestURI();
        if (uri == null || !uri.contains("/camunda/api/admin/auth/user/")) {
            return false;
        }
        int loginIdx = uri.lastIndexOf("/login");
        if (loginIdx < 0) {
            return false;
        }
        return uri.substring(loginIdx + "/login".length()).isEmpty();
    }

    private static boolean isBodyTooLarge(HttpServletRequest request) {
        long contentLength = request.getContentLengthLong();
        return contentLength > MAX_BODY_BYTES;
    }

    private static LoginPayload parseLoginPayload(byte[] body, String contentType) {
        if (body == null || body.length == 0) {
            return null;
        }
        String raw = new String(body, StandardCharsets.UTF_8).trim();
        if (raw.isEmpty()) {
            return null;
        }
        if (raw.startsWith("{")) {
            return parseJsonLogin(raw);
        }
        if (contentType != null && contentType.toLowerCase().contains("application/json")) {
            return parseJsonLogin(raw);
        }
        LoginPayload form = parseFormLogin(raw);
        if (form != null) {
            return form;
        }
        return parseJsonLogin(raw);
    }

    private static LoginPayload parseJsonLogin(String raw) {
        try {
            return OBJECT_MAPPER.readValue(raw, LoginPayload.class);
        } catch (IOException ex) {
            log.debug("Could not parse Camunda JSON login payload: {}", ex.getMessage());
            return null;
        }
    }

    private static LoginPayload parseFormLogin(String raw) {
        String username = null;
        String password = null;
        for (String pair : raw.split("&")) {
            int eq = pair.indexOf('=');
            if (eq <= 0) {
                continue;
            }
            String key = decode(pair.substring(0, eq));
            String value = decode(pair.substring(eq + 1));
            if ("username".equalsIgnoreCase(key)) {
                username = value;
            } else if ("password".equalsIgnoreCase(key)) {
                password = value;
            }
        }
        if (username == null && password == null) {
            return null;
        }
        return new LoginPayload(username, password);
    }

    private static String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record LoginPayload(String username, String password) {
    }

    private static final class CachedBodyHttpServletRequest extends HttpServletRequestWrapper {

        private final byte[] body;

        private CachedBodyHttpServletRequest(HttpServletRequest request) throws IOException {
            super(request);
            body = request.getInputStream().readNBytes(MAX_BODY_BYTES + 1);
            if (body.length > MAX_BODY_BYTES) {
                throw new IOException("Request body exceeds " + MAX_BODY_BYTES + " bytes");
            }
        }

        byte[] getBody() {
            return body;
        }

        @Override
        public int getContentLength() {
            return body.length;
        }

        @Override
        public long getContentLengthLong() {
            return body.length;
        }

        @Override
        public ServletInputStream getInputStream() {
            final ByteArrayInputStream inputStream = new ByteArrayInputStream(body);
            return new ServletInputStream() {
                @Override
                public boolean isFinished() {
                    return inputStream.available() == 0;
                }

                @Override
                public boolean isReady() {
                    return true;
                }

                @Override
                public void setReadListener(ReadListener readListener) {
                    // not used
                }

                @Override
                public int read() {
                    return inputStream.read();
                }

                @Override
                public int read(byte[] b, int off, int len) {
                    return inputStream.read(b, off, len);
                }
            };
        }

        @Override
        public BufferedReader getReader() {
            return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
        }
    }
}
