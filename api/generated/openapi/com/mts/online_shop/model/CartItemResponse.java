package com.mts.online_shop.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.mts.online_shop.model.Product;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * CartItemResponse
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-04-06T13:29:03.673572600+03:00[Europe/Moscow]")
public class CartItemResponse {

  private Long itemId;

  private Product product;

  /**
   * Default constructor
   * @deprecated Use {@link CartItemResponse#CartItemResponse(Long, Product)}
   */
  @Deprecated
  public CartItemResponse() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public CartItemResponse(Long itemId, Product product) {
    this.itemId = itemId;
    this.product = product;
  }

  public CartItemResponse itemId(Long itemId) {
    this.itemId = itemId;
    return this;
  }

  /**
   * Get itemId
   * @return itemId
  */
  @NotNull 
  @Schema(name = "itemId", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("itemId")
  public Long getItemId() {
    return itemId;
  }

  public void setItemId(Long itemId) {
    this.itemId = itemId;
  }

  public CartItemResponse product(Product product) {
    this.product = product;
    return this;
  }

  /**
   * Get product
   * @return product
  */
  @NotNull @Valid 
  @Schema(name = "product", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("product")
  public Product getProduct() {
    return product;
  }

  public void setProduct(Product product) {
    this.product = product;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    CartItemResponse cartItemResponse = (CartItemResponse) o;
    return Objects.equals(this.itemId, cartItemResponse.itemId) &&
        Objects.equals(this.product, cartItemResponse.product);
  }

  @Override
  public int hashCode() {
    return Objects.hash(itemId, product);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class CartItemResponse {\n");
    sb.append("    itemId: ").append(toIndentedString(itemId)).append("\n");
    sb.append("    product: ").append(toIndentedString(product)).append("\n");
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

