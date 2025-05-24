package com.bsuir.bankliquiditserver.dao;

import com.bsuir.bankliquiditserver.db.DatabaseConnector;
import com.bsuir.bankliquiditserver.model.User;
import com.bsuir.bankliquiditserver.model.Role;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

// UserDao теперь реализует GenericDao и также включает специфичные методы
public class UserDao implements GenericDao<User, Integer> {
    private static final Logger LOGGER = LoggerFactory.getLogger(UserDao.class);

    // SQL запросы для GenericDao методов
    private static final String SELECT_BY_ID_SQL = "SELECT id, username, password_hash, full_name, email, role_id, is_active, created_at FROM users WHERE id = ?;";
    private static final String SELECT_ALL_SQL = "SELECT id, username, password_hash, full_name, email, role_id, is_active, created_at FROM users ORDER BY username;";
    private static final String INSERT_SQL = "INSERT INTO users (username, password_hash, full_name, email, role_id, is_active, created_at) VALUES (?, ?, ?, ?, ?, ?, ?);";
    // Обновляем все поля, кроме password_hash (он обновляется отдельным методом) и created_at
    private static final String UPDATE_SQL = "UPDATE users SET username = ?, full_name = ?, email = ?, role_id = ?, is_active = ? WHERE id = ?;";
    private static final String DELETE_BY_ID_SQL = "DELETE FROM users WHERE id = ?;";

    // SQL запросы для специфичных методов UserDao
    private static final String SELECT_BY_USERNAME_SQL = "SELECT id, username, password_hash, full_name, email, role_id, is_active, created_at FROM users WHERE username = ?;";
    private static final String SELECT_BY_EMAIL_SQL = "SELECT id, username, password_hash, full_name, email, role_id, is_active, created_at FROM users WHERE email = ?;";
    private static final String UPDATE_PASSWORD_SQL = "UPDATE users SET password_hash = ? WHERE id = ?;";
    private static final String UPDATE_ACTIVE_STATUS_SQL = "UPDATE users SET is_active = ? WHERE id = ?;";
    private static final String UPDATE_ROLE_SQL = "UPDATE users SET role_id = ? WHERE id = ?;";
    private static final String COUNT_USERS_BY_ROLE_ID_SQL = "SELECT COUNT(*) FROM users WHERE role_id = ?;";

    @Override
    public Optional<User> findById(Integer id) throws SQLException {
        User user = null;
        try (Connection connection = DatabaseConnector.getConnection();
             PreparedStatement ps = connection.prepareStatement(SELECT_BY_ID_SQL)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                user = mapResultSetToUser(rs);
            }
        } catch (SQLException e) {
            LOGGER.error("Error finding user by id {}: {}", id, e.getMessage());
            throw e; // Пробрасываем исключение дальше
        }
        return Optional.ofNullable(user);
    }

    @Override
    public List<User> findAll() throws SQLException {
        List<User> users = new ArrayList<>();
        try (Connection connection = DatabaseConnector.getConnection();
             PreparedStatement ps = connection.prepareStatement(SELECT_ALL_SQL);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                users.add(mapResultSetToUser(rs));
            }
        } catch (SQLException e) {
            LOGGER.error("Error finding all users: {}", e.getMessage());
            throw e;
        }
        return users;
    }

    @Override
    public User save(User user) throws SQLException {
        if (user.getCreatedAt() == null) {
            user.setCreatedAt(LocalDateTime.now()); // Устанавливаем время создания, если не задано
        }
        try (Connection connection = DatabaseConnector.getConnection();
             PreparedStatement ps = connection.prepareStatement(INSERT_SQL, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, user.getUsername());
            ps.setString(2, user.getPasswordHash());
            ps.setString(3, user.getFullName());
            ps.setString(4, user.getEmail());
            ps.setInt(5, user.getRoleId());
            ps.setBoolean(6, user.isActive());
            ps.setTimestamp(7, Timestamp.valueOf(user.getCreatedAt()));

            int affectedRows = ps.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating user failed, no rows affected.");
            }
            try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    user.setId(generatedKeys.getInt(1));
                } else {
                    throw new SQLException("Creating user failed, no ID obtained.");
                }
            }
            LOGGER.info("User saved: {}", user.getUsername());
            return user;
        } catch (SQLException e) {
            LOGGER.error("Error saving user {}: {}", user.getUsername(), e.getMessage());
            // Обработка ошибки уникальности (код 23505 для PostgreSQL)
            if ("23505".equals(e.getSQLState())) {
                if (e.getMessage().toLowerCase().contains("users_username_key")) {
                    throw new SQLException("Username '" + user.getUsername() + "' already exists.", e.getSQLState(), e.getErrorCode(), e);
                } else if (e.getMessage().toLowerCase().contains("users_email_key")) {
                    throw new SQLException("Email '" + user.getEmail() + "' already exists.", e.getSQLState(), e.getErrorCode(), e);
                }
            }
            throw e;
        }
    }

    @Override
    public void update(User user) throws SQLException {
        try (Connection connection = DatabaseConnector.getConnection();
             PreparedStatement ps = connection.prepareStatement(UPDATE_SQL)) {
            ps.setString(1, user.getUsername());
            ps.setString(2, user.getFullName());
            ps.setString(3, user.getEmail());
            ps.setInt(4, user.getRoleId());
            ps.setBoolean(5, user.isActive());
            ps.setInt(6, user.getId()); // WHERE id = ?

            int affectedRows = ps.executeUpdate();
            if (affectedRows == 0) {
                // Можно бросать свое исключение EntityNotFoundException, если такой пользователь не найден для обновления
                throw new SQLException("Updating user failed, no user found with ID: " + user.getId() + " or no data changed.");
            }
            LOGGER.info("User updated: {}", user.getUsername());
        } catch (SQLException e) {
            LOGGER.error("Error updating user {}: {}", user.getUsername(), e.getMessage());
            if ("23505".equals(e.getSQLState())) {
                if (e.getMessage().toLowerCase().contains("users_username_key")) {
                    throw new SQLException("Cannot update: Username '" + user.getUsername() + "' already exists for another user.", e.getSQLState(), e.getErrorCode(), e);
                } else if (e.getMessage().toLowerCase().contains("users_email_key")) {
                    throw new SQLException("Cannot update: Email '" + user.getEmail() + "' already exists for another user.", e.getSQLState(), e.getErrorCode(), e);
                }
            }
            throw e;
        }
    }

    @Override
    public void deleteById(Integer id) throws SQLException {
        try (Connection connection = DatabaseConnector.getConnection();
             PreparedStatement ps = connection.prepareStatement(DELETE_BY_ID_SQL)) {
            ps.setInt(1, id);
            int affectedRows = ps.executeUpdate();
            if (affectedRows == 0) {
                // Можно логировать как WARN, если это не считается критической ошибкой
                LOGGER.warn("No user found with ID {} to delete.", id);
            } else {
                LOGGER.info("User deleted with ID: {}", id);
            }
        } catch (SQLException e) {
            LOGGER.error("Error deleting user by id {}: {}", id, e.getMessage());
            throw e;
        }
    }

    // --- Специфичные методы для UserDao ---

    public Optional<User> findByUsername(String username) throws SQLException {
        User user = null;
        try (Connection connection = DatabaseConnector.getConnection();
             PreparedStatement ps = connection.prepareStatement(SELECT_BY_USERNAME_SQL)) {
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                user = mapResultSetToUser(rs);
            }
        } catch (SQLException e) {
            LOGGER.error("Error finding user by username {}: {}", username, e.getMessage());
            throw e;
        }
        return Optional.ofNullable(user);
    }

    public Optional<User> findByEmail(String email) throws SQLException {
        User user = null;
        try (Connection connection = DatabaseConnector.getConnection();
             PreparedStatement ps = connection.prepareStatement(SELECT_BY_EMAIL_SQL)) {
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                user = mapResultSetToUser(rs);
            }
        } catch (SQLException e) {
            LOGGER.error("Error finding user by email {}: {}", email, e.getMessage());
            throw e;
        }
        return Optional.ofNullable(user);
    }

    public void updatePassword(int userId, String newPasswordHash) throws SQLException {
        try (Connection connection = DatabaseConnector.getConnection();
             PreparedStatement ps = connection.prepareStatement(UPDATE_PASSWORD_SQL)) {
            ps.setString(1, newPasswordHash);
            ps.setInt(2, userId);
            int affectedRows = ps.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Updating password failed, no user found with ID: " + userId);
            }
            LOGGER.info("Password updated for user ID: {}", userId);
        } catch (SQLException e) {
            LOGGER.error("Error updating password for user ID {}: {}", userId, e.getMessage());
            throw e;
        }
    }

    public void updateActiveStatus(int userId, boolean isActive) throws SQLException {
        try (Connection connection = DatabaseConnector.getConnection();
             PreparedStatement ps = connection.prepareStatement(UPDATE_ACTIVE_STATUS_SQL)) {
            ps.setBoolean(1, isActive);
            ps.setInt(2, userId);
            int affectedRows = ps.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Updating active status failed, no user found with ID: " + userId);
            }
            LOGGER.info("Active status for user ID {} set to {}", userId, isActive);
        } catch (SQLException e) {
            LOGGER.error("Error updating active status for user ID {}: {}", userId, e.getMessage());
            throw e;
        }
    }

    public void updateRole(int userId, int roleId) throws SQLException {
        try (Connection connection = DatabaseConnector.getConnection();
             PreparedStatement ps = connection.prepareStatement(UPDATE_ROLE_SQL)) {
            ps.setInt(1, roleId);
            ps.setInt(2, userId);
            int affectedRows = ps.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Updating role failed, no user found with ID: " + userId);
            }
            LOGGER.info("Role ID for user ID {} set to {}", userId, roleId);
        } catch (SQLException e) {
            LOGGER.error("Error updating role for user ID {}: {}", userId, e.getMessage());
            throw e;
        }
    }

    // Вспомогательный метод для маппинга ResultSet в объект User
    private User mapResultSetToUser(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getInt("id"));
        user.setUsername(rs.getString("username"));
        user.setPasswordHash(rs.getString("password_hash"));
        user.setFullName(rs.getString("full_name"));
        user.setEmail(rs.getString("email"));
        user.setRoleId(rs.getInt("role_id"));
        user.setActive(rs.getBoolean("is_active"));
        Timestamp createdAtTs = rs.getTimestamp("created_at");
        if (createdAtTs != null) {
            user.setCreatedAt(createdAtTs.toLocalDateTime());
        }
        // Объект Role здесь не загружается, чтобы избежать N+1.
        // Сервисный слой будет отвечать за его загрузку при необходимости.
        return user;
    }

    public long countUsersByRoleId(int roleId) throws SQLException {
        try (Connection connection = DatabaseConnector.getConnection();
             PreparedStatement ps = connection.prepareStatement(COUNT_USERS_BY_ROLE_ID_SQL)) {
            ps.setInt(1, roleId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getLong(1);
            }
            return 0;
        } catch (SQLException e) {
            LOGGER.error("Error counting users by role_id {}: {}", roleId, e.getMessage());
            throw e;
        }
    }
// ...
}