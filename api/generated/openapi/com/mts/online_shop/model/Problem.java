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
 * RFC 7807 / RFC 9457 — структурированное описание ошибки
 */

@Schema(name = "Problem", description = "RFC 7807 / RFC 9457 — структурированное описание ошибки")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-04-06T13:29:03.673572600+03:00[Europe/Moscow]")
public class Problem {

  private String type;

  private String title;

  private Integer status;

  private String detail;

  private String instance;

  /**
   * Default constructor
   * @deprecated Use {@link Problem#Problem(String, String, Integer)}
   */
  @Deprecated
  public Problem() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public Problem(String type, String title, Integer status) {
    this.type = type;
    this.title = title;
    this.status = status;
  }

  public Problem type(String type) {
    this.type = type;
    return this;
  }

  /**
   * URI типа проблемы
   * @return type
  */
  @NotNull 
  @Schema(name = "type", example = "https://api.example.com/errors/not-found", description = "URI типа проблемы", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("type")
  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public Problem title(String title) {
    this.title = title;
    return this;
  }

  /**
   * Краткое описание проблемы
   * @return title
  */
  @NotNull 
  @Schema(name = "title", description = "Краткое описание проблемы", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("title")
  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public Problem status(Integer status) {
    this.status = status;
    return this;
  }

  /**
   * HTTP-код ответа
   * @return status
  */
  @NotNull 
  @Schema(name = "status", example = "404", description = "HTTP-код ответа", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("status")
  public Integer getStatus() {
    return status;
  }

  public void setStatus(Integer status) {
    this.status = status;
  }

  public Problem detail(String detail) {
    this.detail = detail;
    return this;
  }

  /**
   * Детали для конкретного случая
   * @return detail
  */
  
  @Schema(name = "detail", description = "Детали для конкретного случая", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("detail")
  public String getDetail() {
    return detail;
  }

  public void setDetail(String detail) {
    this.detail = detail;
  }

  public Problem instance(String instance) {
    this.instance = instance;
    return this;
  }

  /**
   * URI конкретного запроса
   * @return instance
  */
  
  @Schema(name = "instance", description = "URI конкретного запроса", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("instance")
  public String getInstance() {
    return instance;
  }

  public void setInstance(String instance) {
    this.instance = instance;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Problem problem = (Problem) o;
    return Objects.equals(this.type, problem.type) &&
        Objects.equals(this.title, problem.title) &&
        Objects.equals(this.status, problem.status) &&
        Objects.equals(this.detail, problem.detail) &&
        Objects.equals(this.instance, problem.instance);
  }

  @Override
  public int hashCode() {
    return Objects.hash(type, title, status, detail, instance);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class Problem {\n");
    sb.append("    type: ").append(toIndentedString(type)).append("\n");
    sb.append("    title: ").append(toIndentedString(title)).append("\n");
    sb.append("    status: ").append(toIndentedString(status)).append("\n");
    sb.append("    detail: ").append(toIndentedString(detail)).append("\n");
    sb.append("    instance: ").append(toIndentedString(instance)).append("\n");
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

