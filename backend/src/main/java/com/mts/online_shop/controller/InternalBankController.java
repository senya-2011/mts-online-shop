package com.mts.online_shop.controller;

import com.mts.online_shop.camunda.BpmUserService;
import com.mts.online_shop.model.MessageResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/internal/bank")
public class InternalBankController {

    private final BpmUserService bpmUserService;

    public InternalBankController(BpmUserService bpmUserService) {
        this.bpmUserService = bpmUserService;
    }

    @PostMapping("/payment-callback")
    public ResponseEntity<MessageResponse> callback(@RequestBody Map<String, Object> payload) {
        Number orderIdRaw = (Number) payload.get("orderId");
        Object successRaw = payload.get("success");
        if (orderIdRaw == null || successRaw == null) {
            throw new IllegalArgumentException("orderId and success are required");
        }
        boolean success = Boolean.parseBoolean(successRaw.toString());
        bpmUserService.correlatePaymentCallback(orderIdRaw.longValue(), success);
        MessageResponse response = new MessageResponse();
        response.setMessage("Callback correlated");
        return ResponseEntity.ok(response);
    }
}
