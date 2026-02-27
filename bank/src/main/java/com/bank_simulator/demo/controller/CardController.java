package com.bank_simulator.demo.controller;

import com.bank_simulator.demo.api.CardsApi;
import com.bank_simulator.demo.model.CardsResponse;
import com.bank_simulator.demo.model.PaymentRequest;
import com.bank_simulator.demo.model.PaymentResponse;
import com.bank_simulator.demo.service.CardService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CardController implements CardsApi {

    private final CardService cardService;

    public CardController(CardService cardService) {
        this.cardService = cardService;
    }

    @Override
    public ResponseEntity<CardsResponse> getCards() {
        return ResponseEntity.ok(cardService.getAllCards());
    }

    @Override
    public ResponseEntity<CardsResponse> randomizeBalances() {
        return ResponseEntity.ok(cardService.randomizeBalances());
    }

    @Override
    public ResponseEntity<PaymentResponse> pay(PaymentRequest paymentRequest) {
        PaymentResponse response = cardService.processPayment(paymentRequest);
        HttpStatus status = Boolean.TRUE.equals(response.getApproved()) ? HttpStatus.OK : HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status).body(response);
    }
}


