package com.mts.online_shop.simulator.bank;

import com.mts.online_shop.exception.InvalidPaymentDataException;
import com.mts.online_shop.model.PaymentRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

@Service
public class BankSimulator {

    private static final Logger log = LoggerFactory.getLogger(BankSimulator.class);
    private static final Pattern CARD_NUMBER = Pattern.compile("^[0-9]{16}$");
    private static final Pattern EXPIRES_AT = Pattern.compile("^\\d{2}/\\d{2}$");
    private static final Pattern CVV = Pattern.compile("^[0-9]{3,4}$");

    public boolean doPayment(PaymentRequest paymentRequest, float totalPrice) {
        validatePaymentRequest(paymentRequest, totalPrice);
        boolean ok = bankLogic(paymentRequest, totalPrice);
        log.debug("doPayment totalPrice={} result={}", totalPrice, ok);
        return ok;
    }

    private void validatePaymentRequest(PaymentRequest paymentRequest, float totalPrice) {
        if (paymentRequest == null) {
            throw new InvalidPaymentDataException("Payment data is required");
        }
        if (totalPrice < 0) {
            throw new InvalidPaymentDataException("Total price must be non-negative");
        }

        String cardNumber = paymentRequest.getCardNumber();
        if (cardNumber == null || cardNumber.isBlank()) {
            throw new InvalidPaymentDataException("Card number is required");
        }
        if (!CARD_NUMBER.matcher(cardNumber.trim()).matches()) {
            throw new InvalidPaymentDataException("Card number must be exactly 16 digits");
        }

        String expiresAt = paymentRequest.getExpiresAt();
        if (expiresAt == null || expiresAt.isBlank()) {
            throw new InvalidPaymentDataException("Expiry date (MM/YY) is required");
        }
        if (!EXPIRES_AT.matcher(expiresAt.trim()).matches()) {
            throw new InvalidPaymentDataException("Expiry date must be in format MM/YY");
        }

        String cvv = paymentRequest.getCvv();
        if (cvv == null || cvv.isBlank()) {
            throw new InvalidPaymentDataException("CVV is required");
        }
        if (!CVV.matcher(cvv.trim()).matches()) {
            throw new InvalidPaymentDataException("CVV must be 3 or 4 digits");
        }
    }

    private boolean bankLogic(PaymentRequest paymentRequest, float totalPrice) {
        return "111".equals(paymentRequest.getCvv());
    }
}
