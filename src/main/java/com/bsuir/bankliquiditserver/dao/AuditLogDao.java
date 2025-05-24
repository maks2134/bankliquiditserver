package com.bsuir.bankliquiditserver.dao;

import com.bsuir.bankliquiditserver.db.DatabaseConnector;
import com.bsuir.bankliquiditserver.model.AuditLogEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AuditLogDao implements GenericDao<AuditLogEntry, Integer> {
    private static final Logger LOGGER = LoggerFactory.getLogger(AuditLogDao.class);

    private static final String SELECT_BY_ID = "SELECT id, user_id, action_type, details, ip_address, timestamp, success FROM audit_log WHERE id = ?;";
    private static final String SELECT_ALL = "SELECT id, user_id, action_type, details, ip_address, timestamp, success FROM audit_log ORDER BY timestamp DESC;";
    private static final String INSERT = "INSERT INTO audit_log (user_id, action_type, details, ip_address, timestamp, success) VALUES (?, ?, ?, ?, ?, ?);";
    // Обновление и удаление для audit_log обычно не требуются или ограничены
    // Для GenericDao они нужны, но можно реализовать их как не поддерживаемые или с ограничениями
    private static final String DELETE_BY_ID = "DELETE FROM audit_log WHERE id = ?;"; // Обычно не используется
    private static final String SELECT_BY_USER_ID = "SELECT id, user_id, action_type, details, ip_address, timestamp, success FROM audit_log WHERE user_id = ? ORDER BY timestamp DESC;";
    private static final String SELECT_BY_ACTION_TYPE = "SELECT id, user_id, action_type, details, ip_address, timestamp, success FROM audit_log WHERE action_type = ? ORDER BY timestamp DESC;";


    @Override
    public Optional<AuditLogEntry> findById(Integer id) throws SQLException {
        AuditLogEntry entry = null;
        try (Connection connection = DatabaseConnector.getConnection();
             PreparedStatement ps = connection.prepareStatement(SELECT_BY_ID)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                entry = mapResultSetToAuditLogEntry(rs);
            }
        } catch (SQLException e) {
            LOGGER.error("Error finding AuditLogEntry by id {}: {}", id, e.getMessage());
            throw e;
        }
        return Optional.ofNullable(entry);
    }

    @Override
    public List<AuditLogEntry> findAll() throws SQLException {
        List<AuditLogEntry> entries = new ArrayList<>();
        try (Connection connection = DatabaseConnector.getConnection();
             PreparedStatement ps = connection.prepareStatement(SELECT_ALL);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                entries.add(mapResultSetToAuditLogEntry(rs));
            }
        } catch (SQLException e) {
            LOGGER.error("Error finding all AuditLogEntries: {}", e.getMessage());
            throw e;
        }
        return entries;
    }

    public List<AuditLogEntry> findByUserId(Integer userId) throws SQLException {
        List<AuditLogEntry> entries = new ArrayList<>();
        try (Connection connection = DatabaseConnector.getConnection();
             PreparedStatement ps = connection.prepareStatement(SELECT_BY_USER_ID)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                entries.add(mapResultSetToAuditLogEntry(rs));
            }
        } catch (SQLException e) {
            LOGGER.error("Error finding AuditLogEntries by user_id {}: {}", userId, e.getMessage());
            throw e;
        }
        return entries;
    }

    public List<AuditLogEntry> findByActionType(String actionType) throws SQLException {
        List<AuditLogEntry> entries = new ArrayList<>();
        try (Connection connection = DatabaseConnector.getConnection();
             PreparedStatement ps = connection.prepareStatement(SELECT_BY_ACTION_TYPE)) {
            ps.setString(1, actionType);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                entries.add(mapResultSetToAuditLogEntry(rs));
            }
        } catch (SQLException e) {
            LOGGER.error("Error finding AuditLogEntries by action_type {}: {}", actionType, e.getMessage());
            throw e;
        }
        return entries;
    }

    @Override
    public AuditLogEntry save(AuditLogEntry entry) throws SQLException {
        try (Connection connection = DatabaseConnector.getConnection();
             PreparedStatement ps = connection.prepareStatement(INSERT, Statement.RETURN_GENERATED_KEYS)) {
            if (entry.getUserId() != null) {
                ps.setInt(1, entry.getUserId());
            } else {
                ps.setNull(1, Types.INTEGER);
            }
            ps.setString(2, entry.getActionType());
            ps.setString(3, entry.getDetails());
            ps.setString(4, entry.getIpAddress());
            ps.setTimestamp(5, entry.getTimestamp() != null ? Timestamp.valueOf(entry.getTimestamp()) : Timestamp.valueOf(java.time.LocalDateTime.now()));
            if (entry.getSuccess() != null) {
                ps.setBoolean(6, entry.getSuccess());
            } else {
                ps.setNull(6, Types.BOOLEAN);
            }


            int affectedRows = ps.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating AuditLogEntry failed, no rows affected.");
            }
            try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    entry.setId(generatedKeys.getInt(1));
                } else {
                    throw new SQLException("Creating AuditLogEntry failed, no ID obtained.");
                }
            }
            LOGGER.info("AuditLogEntry saved: {}", entry);
            return entry;
        } catch (SQLException e) {
            LOGGER.error("Error saving AuditLogEntry {}: {}", entry, e.getMessage());
            throw e;
        }
    }

    @Override
    public void update(AuditLogEntry entity) throws SQLException {
        // Обычно записи аудита не обновляются
        LOGGER.warn("Updating audit log entries is generally not supported or recommended.");
        throw new UnsupportedOperationException("Updating audit log entries is not supported.");
    }

    @Override
    public void deleteById(Integer id) throws SQLException {
        // Удаление записей аудита должно быть строго контролируемым или запрещено
        // Для примера, реализуем, но в реальном приложении это может быть отключено
        try (Connection connection = DatabaseConnector.getConnection();
             PreparedStatement ps = connection.prepareStatement(DELETE_BY_ID)) {
            ps.setInt(1, id);
            int affectedRows = ps.executeUpdate();
            if (affectedRows == 0) {
                LOGGER.warn("No AuditLogEntry found with ID {} to delete.", id);
            } else {
                LOGGER.info("AuditLogEntry deleted with ID: {}", id);
            }
        } catch (SQLException e) {
            LOGGER.error("Error deleting AuditLogEntry by id {}: {}", id, e.getMessage());
            throw e;
        }
    }

    private AuditLogEntry mapResultSetToAuditLogEntry(ResultSet rs) throws SQLException {
        AuditLogEntry entry = new AuditLogEntry();
        entry.setId(rs.getInt("id"));
        entry.setUserId(rs.getObject("user_id", Integer.class));
        entry.setActionType(rs.getString("action_type"));
        entry.setDetails(rs.getString("details"));
        entry.setIpAddress(rs.getString("ip_address"));
        Timestamp timestampTs = rs.getTimestamp("timestamp");
        if (timestampTs != null) {
            entry.setTimestamp(timestampTs.toLocalDateTime());
        }
        entry.setSuccess(rs.getObject("success", Boolean.class));
        // Загрузка связанного объекта User - задача сервисного слоя
        return entry;
    }
}