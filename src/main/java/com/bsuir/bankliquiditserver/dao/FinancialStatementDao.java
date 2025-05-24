package com.bsuir.bankliquiditserver.dao;

import com.bsuir.bankliquiditserver.db.DatabaseConnector;
import com.bsuir.bankliquiditserver.model.FinancialStatement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class FinancialStatementDao implements GenericDao<FinancialStatement, Integer> {
    private static final Logger LOGGER = LoggerFactory.getLogger(FinancialStatementDao.class);

    private static final String SELECT_BY_ID = "SELECT id, bank_id, report_date, statement_type, currency, created_at, created_by_user_id FROM financial_statements WHERE id = ?;";
    private static final String SELECT_ALL = "SELECT id, bank_id, report_date, statement_type, currency, created_at, created_by_user_id FROM financial_statements;";
    private static final String INSERT = "INSERT INTO financial_statements (bank_id, report_date, statement_type, currency, created_at, created_by_user_id) VALUES (?, ?, ?, ?, ?, ?);";
    private static final String UPDATE = "UPDATE financial_statements SET bank_id = ?, report_date = ?, statement_type = ?, currency = ?, created_by_user_id = ? WHERE id = ?;";
    private static final String DELETE_BY_ID = "DELETE FROM financial_statements WHERE id = ?;";
    private static final String SELECT_BY_BANK_ID = "SELECT id, bank_id, report_date, statement_type, currency, created_at, created_by_user_id FROM financial_statements WHERE bank_id = ? ORDER BY report_date DESC;";
    private static final String SELECT_BY_BANK_ID_AND_DATE = "SELECT id, bank_id, report_date, statement_type, currency, created_at, created_by_user_id FROM financial_statements WHERE bank_id = ? AND report_date = ? AND statement_type = ?;";


    @Override
    public Optional<FinancialStatement> findById(Integer id) throws SQLException {
        FinancialStatement statement = null;
        try (Connection connection = DatabaseConnector.getConnection();
             PreparedStatement ps = connection.prepareStatement(SELECT_BY_ID)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                statement = mapResultSetToFinancialStatement(rs);
            }
        } catch (SQLException e) {
            LOGGER.error("Error finding FinancialStatement by id {}: {}", id, e.getMessage());
            throw e;
        }
        return Optional.ofNullable(statement);
    }

    @Override
    public List<FinancialStatement> findAll() throws SQLException {
        List<FinancialStatement> statements = new ArrayList<>();
        try (Connection connection = DatabaseConnector.getConnection();
             PreparedStatement ps = connection.prepareStatement(SELECT_ALL);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                statements.add(mapResultSetToFinancialStatement(rs));
            }
        } catch (SQLException e) {
            LOGGER.error("Error finding all FinancialStatements: {}", e.getMessage());
            throw e;
        }
        return statements;
    }

    public List<FinancialStatement> findByBankId(Integer bankId) throws SQLException {
        List<FinancialStatement> statements = new ArrayList<>();
        try (Connection connection = DatabaseConnector.getConnection();
             PreparedStatement ps = connection.prepareStatement(SELECT_BY_BANK_ID)) {
            ps.setInt(1, bankId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                statements.add(mapResultSetToFinancialStatement(rs));
            }
        } catch (SQLException e) {
            LOGGER.error("Error finding FinancialStatements by bank_id {}: {}", bankId, e.getMessage());
            throw e;
        }
        return statements;
    }

    public Optional<FinancialStatement> findByBankIdAndReportDateAndType(Integer bankId, LocalDate reportDate, String statementType) throws SQLException {
        FinancialStatement statement = null;
        try (Connection connection = DatabaseConnector.getConnection();
             PreparedStatement ps = connection.prepareStatement(SELECT_BY_BANK_ID_AND_DATE)) {
            ps.setInt(1, bankId);
            ps.setDate(2, Date.valueOf(reportDate));
            ps.setString(3, statementType);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                statement = mapResultSetToFinancialStatement(rs);
            }
        } catch (SQLException e) {
            LOGGER.error("Error finding FinancialStatement by bank_id {}, report_date {}, type {}: {}", bankId, reportDate, statementType, e.getMessage());
            throw e;
        }
        return Optional.ofNullable(statement);
    }


    @Override
    public FinancialStatement save(FinancialStatement statement) throws SQLException {
        try (Connection connection = DatabaseConnector.getConnection();
             PreparedStatement ps = connection.prepareStatement(INSERT, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, statement.getBankId());
            ps.setDate(2, Date.valueOf(statement.getReportDate()));
            ps.setString(3, statement.getStatementType());
            ps.setString(4, statement.getCurrency());
            ps.setTimestamp(5, statement.getCreatedAt() != null ? Timestamp.valueOf(statement.getCreatedAt()) : Timestamp.valueOf(java.time.LocalDateTime.now()));
            if (statement.getCreatedByUserId() != null) {
                ps.setInt(6, statement.getCreatedByUserId());
            } else {
                ps.setNull(6, Types.INTEGER);
            }

            int affectedRows = ps.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating FinancialStatement failed, no rows affected.");
            }
            try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    statement.setId(generatedKeys.getInt(1));
                } else {
                    throw new SQLException("Creating FinancialStatement failed, no ID obtained.");
                }
            }
            LOGGER.info("FinancialStatement saved: {}", statement);
            // Сохранение StatementItems должно происходить отдельно, возможно, в сервисном слое
            return statement;
        } catch (SQLException e) {
            LOGGER.error("Error saving FinancialStatement {}: {}", statement, e.getMessage());
            throw e;
        }
    }

    @Override
    public void update(FinancialStatement statement) throws SQLException {
        try (Connection connection = DatabaseConnector.getConnection();
             PreparedStatement ps = connection.prepareStatement(UPDATE)) {
            ps.setInt(1, statement.getBankId());
            ps.setDate(2, Date.valueOf(statement.getReportDate()));
            ps.setString(3, statement.getStatementType());
            ps.setString(4, statement.getCurrency());
            if (statement.getCreatedByUserId() != null) {
                ps.setInt(5, statement.getCreatedByUserId());
            } else {
                ps.setNull(5, Types.INTEGER);
            }
            ps.setInt(6, statement.getId());

            int affectedRows = ps.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Updating FinancialStatement failed, no rows affected for ID: " + statement.getId());
            }
            LOGGER.info("FinancialStatement updated: {}", statement);
            // Обновление StatementItems должно происходить отдельно
        } catch (SQLException e) {
            LOGGER.error("Error updating FinancialStatement {}: {}", statement, e.getMessage());
            throw e;
        }
    }

    @Override
    public void deleteById(Integer id) throws SQLException {
        // При удалении FinancialStatement, связанные StatementItem'ы должны быть удалены каскадно (ON DELETE CASCADE в БД)
        // или вручную перед удалением самого отчета.
        // Если в БД настроено ON DELETE CASCADE для statement_items.statement_id, то достаточно этого:
        try (Connection connection = DatabaseConnector.getConnection();
             PreparedStatement ps = connection.prepareStatement(DELETE_BY_ID)) {
            ps.setInt(1, id);
            int affectedRows = ps.executeUpdate();
            if (affectedRows == 0) {
                LOGGER.warn("No FinancialStatement found with ID {} to delete.", id);
            } else {
                LOGGER.info("FinancialStatement deleted with ID: {}", id);
            }
        } catch (SQLException e) {
            LOGGER.error("Error deleting FinancialStatement by id {}: {}", id, e.getMessage());
            throw e;
        }
    }

    private FinancialStatement mapResultSetToFinancialStatement(ResultSet rs) throws SQLException {
        FinancialStatement statement = new FinancialStatement();
        statement.setId(rs.getInt("id"));
        statement.setBankId(rs.getInt("bank_id"));
        statement.setReportDate(rs.getDate("report_date").toLocalDate());
        statement.setStatementType(rs.getString("statement_type"));
        statement.setCurrency(rs.getString("currency"));
        Timestamp createdAtTs = rs.getTimestamp("created_at");
        if (createdAtTs != null) {
            statement.setCreatedAt(createdAtTs.toLocalDateTime());
        }
        statement.setCreatedByUserId(rs.getObject("created_by_user_id", Integer.class)); // getObject для nullable Integer
        // Загрузка Bank и User объектов, а также списка StatementItem'ов - задача сервисного слоя
        return statement;
    }
}