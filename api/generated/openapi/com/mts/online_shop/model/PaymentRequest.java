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
 * PaymentRequest
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-04-07T15:57:40.664016+03:00[Europe/Moscow]")
public class PaymentRequest {

  private String cardNumber;

  private String expiresAt;

  private String cvv;

  /**
   * Default constructor
   * @deprecated Use {@link PaymentRequest#PaymentRequest(String, String, String)}
   */
  @Deprecated
  public PaymentRequest() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public PaymentRequest(String cardNumber, String expiresAt, String cvv) {
    this.cardNumber = cardNumber;
    this.expiresAt = expiresAt;
    this.cvv = cvv;
  }

  public PaymentRequest cardNumber(String cardNumber) {
    this.cardNumber = cardNumber;
    return this;
  }

  /**
   * Get cardNumber
   * @return cardNumber
  */
  @NotNull 
  @Schema(name = "cardNumber", example = "1234123412341234", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("cardNumber")
  public String getCardNumber() {
    return cardNumber;
  }

  public void setCardNumber(String cardNumber) {
    this.cardNumber = cardNumber;
  }

  public PaymentRequest expiresAt(String expiresAt) {
    this.expiresAt = expiresAt;
    return this;
  }

  /**
   * Get expiresAt
   * @return expiresAt
  */
  @NotNull 
  @Schema(name = "expiresAt", example = "12/30", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("expiresAt")
  public String getExpiresAt() {
    return expiresAt;
  }

  public void setExpiresAt(String expiresAt) {
    this.expiresAt = expiresAt;
  }

  public PaymentRequest cvv(String cvv) {
    this.cvv = cvv;
    return this;
  }

  /**
   * Get cvv
   * @return cvv
  */
  @NotNull 
  @Schema(name = "cvv", example = "111", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("cvv")
  public String getCvv() {
    return cvv;
  }

  public void setCvv(String cvv) {
    this.cvv = cvv;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    PaymentRequest paymentRequest = (PaymentRequest) o;
    return Objects.equals(this.cardNumber, paymentRequest.cardNumber) &&
        Objects.equals(this.expiresAt, paymentRequest.expiresAt) &&
        Objects.equals(this.cvv, paymentRequest.cvv);
  }

  @Override
  public int hashCode() {
    return Objects.hash(cardNumber, expiresAt, cvv);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class PaymentRequest {\n");
    sb.append("    cardNumber: ").append(toIndentedString(cardNumber)).append("\n");
    sb.append("    expiresAt: ").append(toIndentedString(expiresAt)).append("\n");
    sb.append("    cvv: ").append(toIndentedString(cvv)).append("\n");
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

