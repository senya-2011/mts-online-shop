package com.mts.online_shop.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * Тело запроса к банку POST /api/cards/payments
 */

@Schema(name = "BankPaymentRequest", description = "Тело запроса к банку POST /api/cards/payments")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-04-06T13:29:03.673572600+03:00[Europe/Moscow]")
public class BankPaymentRequest {

  private String cardNumber;

  private String cvv;

  private String expiresAt;

  private Float amount;

  /**
   * Default constructor
   * @deprecated Use {@link BankPaymentRequest#BankPaymentRequest(String, String, String, Float)}
   */
  @Deprecated
  public BankPaymentRequest() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public BankPaymentRequest(String cardNumber, String cvv, String expiresAt, Float amount) {
    this.cardNumber = cardNumber;
    this.cvv = cvv;
    this.expiresAt = expiresAt;
    this.amount = amount;
  }

  public BankPaymentRequest cardNumber(String cardNumber) {
    this.cardNumber = cardNumber;
    return this;
  }

  /**
   * Номер карты, 16 цифр
   * @return cardNumber
  */
  @NotNull 
  @Schema(name = "cardNumber", example = "1111222233334444", description = "Номер карты, 16 цифр", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("cardNumber")
  public String getCardNumber() {
    return cardNumber;
  }

  public void setCardNumber(String cardNumber) {
    this.cardNumber = cardNumber;
  }

  public BankPaymentRequest cvv(String cvv) {
    this.cvv = cvv;
    return this;
  }

  /**
   * CVV, 3–4 цифры
   * @return cvv
  */
  @NotNull 
  @Schema(name = "cvv", example = "111", description = "CVV, 3–4 цифры", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("cvv")
  public String getCvv() {
    return cvv;
  }

  public void setCvv(String cvv) {
    this.cvv = cvv;
  }

  public BankPaymentRequest expiresAt(String expiresAt) {
    this.expiresAt = expiresAt;
    return this;
  }

  /**
   * Срок действия ММ/ГГ
   * @return expiresAt
  */
  @NotNull 
  @Schema(name = "expiresAt", example = "12/30", description = "Срок действия ММ/ГГ", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("expiresAt")
  public String getExpiresAt() {
    return expiresAt;
  }

  public void setExpiresAt(String expiresAt) {
    this.expiresAt = expiresAt;
  }

  public BankPaymentRequest amount(Float amount) {
    this.amount = amount;
    return this;
  }

  /**
   * Сумма платежа
   * @return amount
  */
  @NotNull 
  @Schema(name = "amount", example = "150.5", description = "Сумма платежа", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("amount")
  public Float getAmount() {
    return amount;
  }

  public void setAmount(Float amount) {
    this.amount = amount;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    BankPaymentRequest bankPaymentRequest = (BankPaymentRequest) o;
    return Objects.equals(this.cardNumber, bankPaymentRequest.cardNumber) &&
        Objects.equals(this.cvv, bankPaymentRequest.cvv) &&
        Objects.equals(this.expiresAt, bankPaymentRequest.expiresAt) &&
        Objects.equals(this.amount, bankPaymentRequest.amount);
  }

  @Override
  public int hashCode() {
    return Objects.hash(cardNumber, cvv, expiresAt, amount);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class BankPaymentRequest {\n");
    sb.append("    cardNumber: ").append(toIndentedString(cardNumber)).append("\n");
    sb.append("    cvv: ").append(toIndentedString(cvv)).append("\n");
    sb.append("    expiresAt: ").append(toIndentedString(expiresAt)).append("\n");
    sb.append("    amount: ").append(toIndentedString(amount)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }
}

