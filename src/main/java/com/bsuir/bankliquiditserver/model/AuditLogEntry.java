package com.bsuir.bankliquiditserver.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

public class AuditLogEntry implements Serializable {
    private static final long serialVersionUID = 7L;

    private int id;
    private Integer userId; // Может быть null (например, для системных событий или неудачных логинов)
    private User user; // Объект пользователя
    private String actionType;
    private String details;
    private String ipAddress;
    private LocalDateTime timestamp;
    private Boolean success; // null, если неприменимо

    // Конструкторы, геттеры/сеттеры, equals/hashCode/toString
    public AuditLogEntry() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
        if (user != null) {
            this.userId = user.getId();
        }
    }

    public String getActionType() {
        return actionType;
    }

    public void setActionType(String actionType) {
        this.actionType = actionType;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public Boolean getSuccess() {
        return success;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AuditLogEntry that = (AuditLogEntry) o;
        return id == that.id && Objects.equals(userId, that.userId) && Objects.equals(actionType, that.actionType) && Objects.equals(details, that.details) && Objects.equals(ipAddress, that.ipAddress) && Objects.equals(timestamp, that.timestamp) && Objects.equals(success, that.success);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, userId, actionType, details, ipAddress, timestamp, success);
    }

    @Override
    public String toString() {
        return "AuditLogEntry{" +
                "id=" + id +
                ", userId=" + userId +
                ", actionType='" + actionType + '\'' +
                ", timestamp=" + timestamp +
                ", success=" + success +
                '}';
    }
}