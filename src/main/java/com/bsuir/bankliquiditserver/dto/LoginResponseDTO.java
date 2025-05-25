package com.bsuir.bankliquiditserver.dto;

import java.io.Serializable;
import java.util.Objects;

public class LoginResponseDTO implements Serializable {
    private static final long serialVersionUID = 304L;

    private String token;
    private UserDTO user;

    public LoginResponseDTO() {}

    public LoginResponseDTO(String token, UserDTO user) {
        this.token = token;
        this.user = user;
    }

    // Геттеры
    public String getToken() { return token; }
    public UserDTO getUser() { return user; }

    // Сеттеры
    public void setToken(String token) { this.token = token; }
    public void setUser(UserDTO user) { this.user = user; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LoginResponseDTO that = (LoginResponseDTO) o;
        return Objects.equals(token, that.token) && Objects.equals(user, that.user);
    }

    @Override
    public int hashCode() {
        return Objects.hash(token, user);
    }

    @Override
    public String toString() {
        return "LoginResponseDTO{" +
                "token='" + (token != null ? "present" : "null") + '\'' +
                ", user=" + (user != null ? user.getUsername() : "null") +
                '}';
    }
}