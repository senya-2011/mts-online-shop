package com.mts.online_shop.client.bank;

import com.mts.online_shop.model.PaymentRequest;

import java.math.BigDecimal;

/**
 * Клиент к банковскому сервису (оплата по карте).
 */
public interface BankClient {

    /**
     * Выполнить платёж через банк.
     *
     * @param paymentRequest данные карты (номер, cvv, срок)
     * @param amount         сумма к списанию
     * @return true если платёж одобрен
     * @throws com.mts.online_shop.exception.InvalidPaymentDataException при ошибке валидации или отказе банка
     */
    boolean doPayment(PaymentRequest paymentRequest, BigDecimal amount);
    
    /**
     * Выполнить возврат средств.
     *
     * @param amount сумма к возврату
     * @return true если возврат выполнен успешно
     */
    boolean refundPayment(BigDecimal amount);
}
