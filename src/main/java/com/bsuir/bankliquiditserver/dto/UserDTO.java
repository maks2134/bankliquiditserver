package com.bsuir.bankliquiditserver.dto;

import com.bsuir.bankliquiditserver.model.User;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

public class UserDTO implements Serializable {
    private static final long serialVersionUID = 303L;

    private int id;
    private String username;
    private String fullName;
    private String email;
    private String roleName; // Вместо roleId или объекта Role
    private boolean isActive;
    private LocalDateTime createdAt;

    public UserDTO() {}

    public UserDTO(int id, String username, String fullName, String email, String roleName, boolean isActive, LocalDateTime createdAt) {
        this.id = id;
        this.username = username;
        this.fullName = fullName;
        this.email = email;
        this.roleName = roleName;
        this.isActive = isActive;
        this.createdAt = createdAt;
    }

    // Геттеры
    public int getId() { return id; }
    public String getUsername() { return username; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public String getRoleName() { return roleName; }
    public boolean isActive() { return isActive; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    // Сеттеры
    public void setId(int id) { this.id = id; }
    public void setUsername(String username) { this.username = username; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public void setEmail(String email) { this.email = email; }
    public void setRoleName(String roleName) { this.roleName = roleName; }
    public void setActive(boolean active) { isActive = active; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }


    public static UserDTO fromUser(User user) {
        if (user == null) return null;
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setFullName(user.getFullName());
        dto.setEmail(user.getEmail());
        if (user.getRole() != null) {
            dto.setRoleName(user.getRole().getRoleName());
        } else if (user.getRoleId() > 0) {
            // В идеале, роль должна быть загружена сервисом перед преобразованием в DTO.
            // Если нет, можно оставить roleName null или использовать placeholder.
            // Для простоты сейчас оставим null, если объект Role не загружен.
            // Либо сервис, который готовит User для DTO, должен позаботиться о загрузке роли.
            dto.setRoleName(null); // Или "ROLE_ID_" + user.getRoleId() если нужна какая-то заглушка
        }
        dto.setActive(user.isActive());
        dto.setCreatedAt(user.getCreatedAt());
        return dto;
    }

    // Метод toUser может быть опасен, т.к. DTO не содержит passwordHash.
    // Обычно он не нужен, т.к. User создается или обновляется на сервере из более специфичных DTO или параметров.

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserDTO userDTO = (UserDTO) o;
        return id == userDTO.id &&
                isActive == userDTO.isActive &&
                Objects.equals(username, userDTO.username) &&
                Objects.equals(fullName, userDTO.fullName) &&
                Objects.equals(email, userDTO.email) &&
                Objects.equals(roleName, userDTO.roleName) &&
                Objects.equals(createdAt, userDTO.createdAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, username, fullName, email, roleName, isActive, createdAt);
    }

    @Override
    public String toString() {
        return "UserDTO{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", fullName='" + fullName + '\'' +
                ", email='" + email + '\'' +
                ", roleName='" + roleName + '\'' +
                ", isActive=" + isActive +
                ", createdAt=" + createdAt +
                '}';
    }
}