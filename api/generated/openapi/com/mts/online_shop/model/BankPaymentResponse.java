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
 * Ответ банка на платёж
 */

@Schema(name = "BankPaymentResponse", description = "Ответ банка на платёж")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-04-06T13:29:03.673572600+03:00[Europe/Moscow]")
public class BankPaymentResponse {

  private Boolean approved;

  private String message;

  private Float remainingBalance;

  /**
   * Default constructor
   * @deprecated Use {@link BankPaymentResponse#BankPaymentResponse(Boolean, String)}
   */
  @Deprecated
  public BankPaymentResponse() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public BankPaymentResponse(Boolean approved, String message) {
    this.approved = approved;
    this.message = message;
  }

  public BankPaymentResponse approved(Boolean approved) {
    this.approved = approved;
    return this;
  }

  /**
   * Платёж одобрен
   * @return approved
  */
  @NotNull 
  @Schema(name = "approved", description = "Платёж одобрен", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("approved")
  public Boolean getApproved() {
    return approved;
  }

  public void setApproved(Boolean approved) {
    this.approved = approved;
  }

  public BankPaymentResponse message(String message) {
    this.message = message;
    return this;
  }

  /**
   * Сообщение от банка
   * @return message
  */
  @NotNull 
  @Schema(name = "message", description = "Сообщение от банка", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("message")
  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public BankPaymentResponse remainingBalance(Float remainingBalance) {
    this.remainingBalance = remainingBalance;
    return this;
  }

  /**
   * Оставшийся баланс по карте
   * @return remainingBalance
  */
  
  @Schema(name = "remainingBalance", description = "Оставшийся баланс по карте", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("remainingBalance")
  public Float getRemainingBalance() {
    return remainingBalance;
  }

  public void setRemainingBalance(Float remainingBalance) {
    this.remainingBalance = remainingBalance;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    BankPaymentResponse bankPaymentResponse = (BankPaymentResponse) o;
    return Objects.equals(this.approved, bankPaymentResponse.approved) &&
        Objects.equals(this.message, bankPaymentResponse.message) &&
        Objects.equals(this.remainingBalance, bankPaymentResponse.remainingBalance);
  }

  @Override
  public int hashCode() {
    return Objects.hash(approved, message, remainingBalance);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class BankPaymentResponse {\n");
    sb.append("    approved: ").append(toIndentedString(approved)).append("\n");
    sb.append("    message: ").append(toIndentedString(message)).append("\n");
    sb.append("    remainingBalance: ").append(toIndentedString(remainingBalance)).append("\n");
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

