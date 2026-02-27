package com.bank_simulator.demo.service;

import com.bank_simulator.demo.exception.CardNotFoundException;
import com.bank_simulator.demo.exception.InsufficientFundsException;
import com.bank_simulator.demo.exception.InvalidAmountException;
import com.bank_simulator.demo.mapper.CardMapper;
import com.bank_simulator.demo.model.Card;
import com.bank_simulator.demo.model.CardsResponse;
import com.bank_simulator.demo.model.PaymentRequest;
import com.bank_simulator.demo.model.PaymentResponse;
import com.bank_simulator.demo.repository.CardRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class CardService {

    private final CardRepository cardRepository;
    private final CardMapper cardMapper;

    public CardService(CardRepository cardRepository, CardMapper cardMapper) {
        this.cardRepository = cardRepository;
        this.cardMapper = cardMapper;
    }

    @Transactional(readOnly = true)
    public CardsResponse getAllCards() {
        return cardMapper.toCardsResponse(cardRepository.findAll());
    }

    @Transactional
    public CardsResponse randomizeBalances() {
        List<Card> cards = cardRepository.findAll();
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        for (Card card : cards) {
            double value = rnd.nextDouble(0.0, 20000.0);
            BigDecimal balance = BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
            card.setBalance(balance);
        }
        cardRepository.saveAll(cards);
        return cardMapper.toCardsResponse(cards);
    }

    @Transactional
    public PaymentResponse processPayment(PaymentRequest request) {
        Float amountFloat = request.getAmount();
        if (amountFloat == null || amountFloat <= 0) {
            throw new InvalidAmountException("Amount must be positive");
        }
        BigDecimal amount = BigDecimal.valueOf(amountFloat);

        Card card = cardRepository.findByNumberAndCvvAndExpiresAt(
                        request.getCardNumber(),
                        request.getCvv(),
                        request.getExpiresAt())
                .orElseThrow(() -> new CardNotFoundException("Card not found or data is invalid"));

        PaymentResponse response = new PaymentResponse();
        if (card.getBalance().compareTo(amount) >= 0) {
            card.setBalance(card.getBalance().subtract(amount));
            cardRepository.save(card);
            response.setApproved(true);
            response.setMessage("Payment approved");
            response.setRemainingBalance(card.getBalance().floatValue());
        } else {
            throw new InsufficientFundsException("Insufficient funds", card.getBalance());
        }

        return response;
    }
}


