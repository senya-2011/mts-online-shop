package com.mts.online_shop.camunda;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;


public final class PaymentCardValidator {

    private static final Pattern CARD_PATTERN = Pattern.compile("^[0-9]{16}$");
    private static final Pattern CVV_PATTERN = Pattern.compile("^[0-9]{3}$");
    private static final Pattern EXPIRY_PATTERN = Pattern.compile("^(0[1-9]|1[0-2])/([0-9]{2})$");

    private PaymentCardValidator() {
    }

    public static String normalizeCardNumber(String cardNumber) {
        if (cardNumber == null) {
            return null;
        }
        return cardNumber.replaceAll("[\\s-]", "");
    }

   
    public static String validate(String cardNumber, String cvv, String expiresAt) {
        String normalizedCard = normalizeCardNumber(cardNumber);
        String normalizedCvv = cvv == null ? null : cvv.trim();
        String normalizedExpiry = expiresAt == null ? null : expiresAt.trim();

        List<String> errors = new ArrayList<>();
        if (normalizedCard == null || !CARD_PATTERN.matcher(normalizedCard).matches()) {
            errors.add("Номер карты должен содержать 16 цифр");
        }
        if (normalizedCvv == null || !CVV_PATTERN.matcher(normalizedCvv).matches()) {
            errors.add("CVV должен содержать 3 цифры");
        }
        if (normalizedExpiry == null || !EXPIRY_PATTERN.matcher(normalizedExpiry).matches()) {
            errors.add("Срок действия должен быть в формате MM/YY");
        }
        return errors.isEmpty() ? null : String.join("; ", errors);
    }
}
