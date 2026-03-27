package com.bank_simulator.demo.service;

import com.bank_simulator.demo.exception.CardNotFoundException;
import com.bank_simulator.demo.exception.InsufficientFundsException;
import com.bank_simulator.demo.exception.InvalidAmountException;
import com.bank_simulator.demo.exception.InvalidCardDataException;
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
import java.time.YearMonth;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;

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

    private static final Pattern CARD_NUMBER_DIGITS = Pattern.compile("^[0-9]{16}$");
    private static final Pattern CVV_DIGITS = Pattern.compile("^[0-9]{3,4}$");
    private static final Pattern EXPIRES_AT = Pattern.compile("^(0[1-9]|1[0-2])/[0-9]{2}$");

    @Transactional
    public PaymentResponse processPayment(PaymentRequest request) {
        Float amountFloat = request.getAmount();
        if (amountFloat == null || amountFloat <= 0) {
            throw new InvalidAmountException("Amount must be positive");
        }
        BigDecimal amount = BigDecimal.valueOf(amountFloat);

        String cardNumber = normalizeCardNumber(request.getCardNumber());
        String cvv = request.getCvv() != null ? request.getCvv().trim() : "";
        String expiresAt = request.getExpiresAt() != null ? request.getExpiresAt().trim() : "";

        if (!CARD_NUMBER_DIGITS.matcher(cardNumber).matches()) {
            throw new InvalidCardDataException("Card number must be 16 digits");
        }
        if (!CVV_DIGITS.matcher(cvv).matches()) {
            throw new InvalidCardDataException("CVV must be 3 or 4 digits");
        }
        if (!EXPIRES_AT.matcher(expiresAt).matches()) {
            throw new InvalidCardDataException("Expiry date must be MM/YY (e.g. 12/30)");
        }
        validateExpiryNotPast(expiresAt);

        Card card = cardRepository.findByNumberAndCvvAndExpiresAt(cardNumber, cvv, expiresAt)
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

    private static String normalizeCardNumber(String value) {
        if (value == null) {
            return "";
        }
        return value.replaceAll("\\s", "").trim();
    }

    private static void validateExpiryNotPast(String expiresAt) {
        // expiresAt is MM/YY
        int month = Integer.parseInt(expiresAt.substring(0, 2), 10);
        int year = 2000 + Integer.parseInt(expiresAt.substring(3, 5), 10);
        YearMonth expiry = YearMonth.of(year, month);
        if (expiry.isBefore(YearMonth.now())) {
            throw new InvalidCardDataException("Card has expired");
        }
    }
}


