package com.bsuir.bankliquiditserver.dto;

import java.io.Serializable;
import java.util.Map;

public class ClientRequestDTO implements Serializable {
    private static final long serialVersionUID = 301L;

    private String action;
    private Object payload; // Может быть Map<String, Object> или специфичный DTO для каждой команды
    private String token;   // Для авторизованных запросов

    public ClientRequestDTO() {}

    public ClientRequestDTO(String action, Object payload) {
        this.action = action;
        this.payload = payload;
    }

    public ClientRequestDTO(String action, Object payload, String token) {
        this.action = action;
        this.payload = payload;
        this.token = token;
    }

    // Геттеры и сеттеры
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public Object getPayload() { return payload; }
    public void setPayload(Object payload) { this.payload = payload; }
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    @Override
    public String toString() {
        return "ClientRequestDTO{" +
                "action='" + action + '\'' +
                ", payloadType=" + (payload != null ? payload.getClass().getSimpleName() : "null") +
                ", token='" + (token != null ? "present" : "null") + '\'' +
                '}';
    }
}