package com.bsuir.bankliquiditserver.dao;

import com.bsuir.bankliquiditserver.db.DatabaseConnector;
import com.bsuir.bankliquiditserver.model.Role;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RoleDao implements GenericDao<Role, Integer> {
    private static final Logger LOGGER = LoggerFactory.getLogger(RoleDao.class);

    private static final String SELECT_ROLE_BY_ID = "SELECT id, role_name FROM roles WHERE id = ?;";
    private static final String SELECT_ALL_ROLES = "SELECT id, role_name FROM roles;";
    private static final String INSERT_ROLE = "INSERT INTO roles (role_name) VALUES (?);";
    private static final String UPDATE_ROLE = "UPDATE roles SET role_name = ? WHERE id = ?;";
    private static final String DELETE_ROLE = "DELETE FROM roles WHERE id = ?;";
    private static final String SELECT_ROLE_BY_NAME = "SELECT id, role_name FROM roles WHERE role_name = ?;";


    @Override
    public Optional<Role> findById(Integer id) throws SQLException {
        Role role = null;
        try (Connection connection = DatabaseConnector.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(SELECT_ROLE_BY_ID)) {
            preparedStatement.setInt(1, id);
            ResultSet rs = preparedStatement.executeQuery();
            if (rs.next()) {
                role = mapResultSetToRole(rs);
            }
        } catch (SQLException e) {
            LOGGER.error("Error finding role by id {}: {}", id, e.getMessage());
            throw e; // Пробрасываем исключение дальше или обрабатываем специфично
        }
        return Optional.ofNullable(role);
    }

    public Optional<Role> findByName(String roleName) throws SQLException {
        Role role = null;
        try (Connection connection = DatabaseConnector.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(SELECT_ROLE_BY_NAME)) {
            preparedStatement.setString(1, roleName);
            ResultSet rs = preparedStatement.executeQuery();
            if (rs.next()) {
                role = mapResultSetToRole(rs);
            }
        } catch (SQLException e) {
            LOGGER.error("Error finding role by name {}: {}", roleName, e.getMessage());
            throw e;
        }
        return Optional.ofNullable(role);
    }

    @Override
    public List<Role> findAll() throws SQLException {
        List<Role> roles = new ArrayList<>();
        try (Connection connection = DatabaseConnector.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(SELECT_ALL_ROLES);
             ResultSet rs = preparedStatement.executeQuery()) {
            while (rs.next()) {
                roles.add(mapResultSetToRole(rs));
            }
        } catch (SQLException e) {
            LOGGER.error("Error finding all roles: {}", e.getMessage());
            throw e;
        }
        return roles;
    }

    @Override
    public Role save(Role role) throws SQLException {
        try (Connection connection = DatabaseConnector.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(INSERT_ROLE, Statement.RETURN_GENERATED_KEYS)) {
            preparedStatement.setString(1, role.getRoleName());
            int affectedRows = preparedStatement.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating role failed, no rows affected.");
            }
            try (ResultSet generatedKeys = preparedStatement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    role.setId(generatedKeys.getInt(1));
                } else {
                    throw new SQLException("Creating role failed, no ID obtained.");
                }
            }
            LOGGER.info("Role saved: {}", role);
            return role;
        } catch (SQLException e) {
            LOGGER.error("Error saving role {}: {}", role, e.getMessage());
            throw e;
        }
    }

    @Override
    public void update(Role role) throws SQLException {
        try (Connection connection = DatabaseConnector.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(UPDATE_ROLE)) {
            preparedStatement.setString(1, role.getRoleName());
            preparedStatement.setInt(2, role.getId());
            int affectedRows = preparedStatement.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Updating role failed, no rows affected for ID: " + role.getId());
            }
            LOGGER.info("Role updated: {}", role);
        } catch (SQLException e) {
            LOGGER.error("Error updating role {}: {}", role, e.getMessage());
            throw e;
        }
    }

    @Override
    public void deleteById(Integer id) throws SQLException {
        try (Connection connection = DatabaseConnector.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(DELETE_ROLE)) {
            preparedStatement.setInt(1, id);
            int affectedRows = preparedStatement.executeUpdate();
            if (affectedRows == 0) {
                // Можно считать это ошибкой или просто информацией, что такой записи не было
                LOGGER.warn("No role found with ID {} to delete.", id);
            } else {
                LOGGER.info("Role deleted with ID: {}", id);
            }
        } catch (SQLException e) {
            LOGGER.error("Error deleting role by id {}: {}", id, e.getMessage());
            throw e;
        }
    }

    private Role mapResultSetToRole(ResultSet rs) throws SQLException {
        Role role = new Role();
        role.setId(rs.getInt("id"));
        role.setRoleName(rs.getString("role_name"));
        return role;
    }
}