package com.mts.online_shop.controller;

import com.mts.online_shop.exception.ApiException;
import com.mts.online_shop.model.Problem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.HttpRequestMethodNotSupportedException;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.ConstraintViolationException;
import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.util.stream.Collectors;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String PROBLEM_TYPE_URI = "https://api.mts-online-shop.example/errors";

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<Problem> handleApiException(ApiException e, WebRequest request) {
        if (e.getStatus().is5xxServerError()) {
            log.error("API error: {}", e.getMessage(), e);
        } else {
            log.warn("API error: {}", e.getMessage());
        }
        return problem(e.getStatus(), e.getTitle(), e.getType(), e.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Problem> handleValidation(MethodArgumentNotValidException e, WebRequest request) {
        String detail = e.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .collect(Collectors.joining("; "));
        log.warn("Validation error: {}", detail);
        return problem(HttpStatus.BAD_REQUEST, "Bad Request", "validation-error", detail, request);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Problem> handleConstraintViolation(ConstraintViolationException e, WebRequest request) {
        String detail = e.getConstraintViolations().stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .collect(Collectors.joining("; "));
        log.warn("Constraint violation: {}", detail);
        return problem(HttpStatus.BAD_REQUEST, "Bad Request", "validation-error", detail, request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Problem> handleIllegalArgument(IllegalArgumentException e, WebRequest request) {
        log.warn("Bad request: {}", e.getMessage());
        return problem(HttpStatus.BAD_REQUEST, "Bad Request", "bad-request", e.getMessage(), request);
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<Problem> handleNoHandlerFound(NoHandlerFoundException e, WebRequest request) {
        if (isCamundaInfrastructureRequest(request)) {
            return ResponseEntity.notFound().build();
        }
        log.warn("No handler found: {} {}", e.getHttpMethod(), e.getRequestURL());
        return problem(HttpStatus.NOT_FOUND, "Not Found", "not-found", "No handler for this request", request);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Problem> handleMethodNotAllowed(HttpRequestMethodNotSupportedException e, WebRequest request) {
        log.warn("Method not supported: {} {}", e.getMethod(), request.getDescription(false));
        return problem(HttpStatus.NOT_FOUND, "Not Found", "not-found", "Method not supported", request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Problem> handleGeneric(Exception e, WebRequest request) {
        if (isClientDisconnect(e) || isResponseCommitted(request)) {
            return null;
        }
        if (isCamundaInfrastructureRequest(request)) {
            log.debug("Camunda resource error: {}", e.toString());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        log.error("Unhandled error", e);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error", "internal-error", null, request);
    }

    private static boolean isCamundaInfrastructureRequest(WebRequest request) {
        if (!(request instanceof ServletWebRequest servletRequest)) {
            return false;
        }
        String path = servletRequest.getRequest().getRequestURI();
        return path.contains("/camunda/") || path.contains("/engine-rest/");
    }

    private static boolean isResponseCommitted(WebRequest request) {
        if (request instanceof ServletWebRequest servletRequest) {
            HttpServletResponse response = servletRequest.getResponse();
            return response != null && response.isCommitted();
        }
        return false;
    }

    private static boolean isClientDisconnect(Throwable e) {
        Throwable current = e;
        while (current != null) {
            if (current instanceof ClosedChannelException
                    || current instanceof IOException && "Broken pipe".equalsIgnoreCase(current.getMessage())) {
                return true;
            }
            String message = current.getMessage();
            if (message != null && message.contains("разорвала установленное подключение")) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private static ResponseEntity<Problem> problem(HttpStatus status,
                                                   String title,
                                                   String typeSuffix,
                                                   String detail,
                                                   WebRequest request) {
        Problem p = new Problem();
        p.setType(PROBLEM_TYPE_URI + "/" + typeSuffix);
        p.setTitle(title);
        p.setStatus(status.value());
        p.setDetail(detail);
        if (request != null && request.getDescription(false) != null) {
            p.setInstance(request.getDescription(false).replace("uri=", ""));
        }
        return ResponseEntity.status(status)
                .contentType(MediaType.parseMediaType("application/problem+json"))
                .body(p);
    }
}
