package com.bsuir.bankliquiditserver.dto;

import java.io.Serializable;

public class ServerResponseDTO implements Serializable {
    private static final long serialVersionUID = 302L;

    private Status status;
    private Object data;
    private String errorMessage;

    public enum Status {
        SUCCESS,
        ERROR,
        UNAUTHORIZED, // 401
        FORBIDDEN,    // 403
        BAD_REQUEST,  // 400
        NOT_FOUND     // 404
    }

    public ServerResponseDTO() {}

    private ServerResponseDTO(Status status, Object data, String errorMessage) {
        this.status = status;
        this.data = data;
        this.errorMessage = errorMessage;
    }

    public static ServerResponseDTO success(Object data) {
        return new ServerResponseDTO(Status.SUCCESS, data, null);
    }

    public static ServerResponseDTO error(String errorMessage) {
        return new ServerResponseDTO(Status.ERROR, null, errorMessage);
    }

    public static ServerResponseDTO unauthorized(String message) {
        return new ServerResponseDTO(Status.UNAUTHORIZED, null, message);
    }

    public static ServerResponseDTO forbidden(String message) {
        return new ServerResponseDTO(Status.FORBIDDEN, null, message);
    }

    public static ServerResponseDTO badRequest(String message) {
        return new ServerResponseDTO(Status.BAD_REQUEST, null, message);
    }
    public static ServerResponseDTO notFound(String message) {
        return new ServerResponseDTO(Status.NOT_FOUND, null, message);
    }


    // Геттеры и сеттеры
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public Object getData() { return data; }
    public void setData(Object data) { this.data = data; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    @Override
    public String toString() {
        return "ServerResponseDTO{" +
                "status=" + status +
                ", dataPresent=" + (data != null) +
                ", errorMessage='" + errorMessage + '\'' +
                '}';
    }
}