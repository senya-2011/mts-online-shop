package com.mts.online_shop.model;

/**
 * RFC7807-like problem response payload.
 * Stored locally in backend because OpenAPI schema "Problem"
 * is no longer generated in api/generated sources.
 */
public class Problem {

    private String type;
    private String title;
    private Integer status;
    private String detail;
    private String instance;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

    public String getInstance() {
        return instance;
    }

    public void setInstance(String instance) {
        this.instance = instance;
    }
}
