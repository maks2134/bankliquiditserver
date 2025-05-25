package com.bsuir.bankliquiditserver.dto;

import com.bsuir.bankliquiditserver.model.AuditLogEntry;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

public class AuditLogDTO implements Serializable {
    private static final long serialVersionUID = 306L;

    private int id;
    private Integer userId;
    private String username; // Для отображения
    private String actionType;
    private String details;
    private String ipAddress;
    private LocalDateTime timestamp;
    private Boolean success;

    public AuditLogDTO() {}

    public AuditLogDTO(int id, Integer userId, String username, String actionType, String details, String ipAddress, LocalDateTime timestamp, Boolean success) {
        this.id = id;
        this.userId = userId;
        this.username = username;
        this.actionType = actionType;
        this.details = details;
        this.ipAddress = ipAddress;
        this.timestamp = timestamp;
        this.success = success;
    }

    // Геттеры
    public int getId() { return id; }
    public Integer getUserId() { return userId; }
    public String getUsername() { return username; }
    public String getActionType() { return actionType; }
    public String getDetails() { return details; }
    public String getIpAddress() { return ipAddress; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public Boolean getSuccess() { return success; }

    // Сеттеры
    public void setId(int id) { this.id = id; }
    public void setUserId(Integer userId) { this.userId = userId; }
    public void setUsername(String username) { this.username = username; }
    public void setActionType(String actionType) { this.actionType = actionType; }
    public void setDetails(String details) { this.details = details; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    public void setSuccess(Boolean success) { this.success = success; }


    public static AuditLogDTO fromAuditLogEntry(AuditLogEntry entry) {
        if (entry == null) return null;
        AuditLogDTO dto = new AuditLogDTO();
        dto.setId(entry.getId());
        dto.setUserId(entry.getUserId());
        if (entry.getUser() != null) {
            dto.setUsername(entry.getUser().getUsername());
        } else if (entry.getUserId() != null) {
            dto.setUsername("User ID: " + entry.getUserId()); // Заглушка, если объект User не загружен
        }
        dto.setActionType(entry.getActionType());
        dto.setDetails(entry.getDetails());
        dto.setIpAddress(entry.getIpAddress());
        dto.setTimestamp(entry.getTimestamp());
        dto.setSuccess(entry.getSuccess());
        return dto;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AuditLogDTO that = (AuditLogDTO) o;
        return id == that.id &&
                Objects.equals(userId, that.userId) &&
                Objects.equals(username, that.username) &&
                Objects.equals(actionType, that.actionType) &&
                Objects.equals(details, that.details) &&
                Objects.equals(ipAddress, that.ipAddress) &&
                Objects.equals(timestamp, that.timestamp) &&
                Objects.equals(success, that.success);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, userId, username, actionType, details, ipAddress, timestamp, success);
    }

    @Override
    public String toString() {
        return "AuditLogDTO{" +
                "id=" + id +
                ", userId=" + userId +
                (username != null ? ", username='" + username + '\'' : "") +
                ", actionType='" + actionType + '\'' +
                ", timestamp=" + timestamp +
                ", success=" + success +
                '}';
    }
}