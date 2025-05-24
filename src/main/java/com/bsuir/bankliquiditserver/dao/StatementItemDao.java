package com.bsuir.bankliquiditserver.dao;

import com.bsuir.bankliquiditserver.db.DatabaseConnector;
import com.bsuir.bankliquiditserver.model.StatementItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class StatementItemDao implements GenericDao<StatementItem, Integer> {
    private static final Logger LOGGER = LoggerFactory.getLogger(StatementItemDao.class);

    private static final String SELECT_BY_ID = "SELECT id, statement_id, item_code, item_name, item_value, parent_item_id FROM statement_items WHERE id = ?;";
    private static final String SELECT_ALL = "SELECT id, statement_id, item_code, item_name, item_value, parent_item_id FROM statement_items;";
    private static final String INSERT = "INSERT INTO statement_items (statement_id, item_code, item_name, item_value, parent_item_id) VALUES (?, ?, ?, ?, ?);";
    private static final String UPDATE = "UPDATE statement_items SET statement_id = ?, item_code = ?, item_name = ?, item_value = ?, parent_item_id = ? WHERE id = ?;";
    private static final String DELETE_BY_ID = "DELETE FROM statement_items WHERE id = ?;";
    private static final String SELECT_BY_STATEMENT_ID = "SELECT id, statement_id, item_code, item_name, item_value, parent_item_id FROM statement_items WHERE statement_id = ? ORDER BY item_code, item_name;";
    private static final String DELETE_BY_STATEMENT_ID = "DELETE FROM statement_items WHERE statement_id = ?;";


    @Override
    public Optional<StatementItem> findById(Integer id) throws SQLException {
        StatementItem item = null;
        try (Connection connection = DatabaseConnector.getConnection();
             PreparedStatement ps = connection.prepareStatement(SELECT_BY_ID)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                item = mapResultSetToStatementItem(rs);
            }
        } catch (SQLException e) {
            LOGGER.error("Error finding StatementItem by id {}: {}", id, e.getMessage());
            throw e;
        }
        return Optional.ofNullable(item);
    }

    @Override
    public List<StatementItem> findAll() throws SQLException {
        List<StatementItem> items = new ArrayList<>();
        try (Connection connection = DatabaseConnector.getConnection();
             PreparedStatement ps = connection.prepareStatement(SELECT_ALL);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                items.add(mapResultSetToStatementItem(rs));
            }
        } catch (SQLException e) {
            LOGGER.error("Error finding all StatementItems: {}", e.getMessage());
            throw e;
        }
        return items;
    }

    public List<StatementItem> findByStatementId(Integer statementId) throws SQLException {
        List<StatementItem> items = new ArrayList<>();
        try (Connection connection = DatabaseConnector.getConnection();
             PreparedStatement ps = connection.prepareStatement(SELECT_BY_STATEMENT_ID)) {
            ps.setInt(1, statementId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                items.add(mapResultSetToStatementItem(rs));
            }
        } catch (SQLException e) {
            LOGGER.error("Error finding StatementItems by statement_id {}: {}", statementId, e.getMessage());
            throw e;
        }
        return items;
    }

    @Override
    public StatementItem save(StatementItem item) throws SQLException {
        try (Connection connection = DatabaseConnector.getConnection();
             PreparedStatement ps = connection.prepareStatement(INSERT, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, item.getStatementId());
            ps.setString(2, item.getItemCode());
            ps.setString(3, item.getItemName());
            ps.setBigDecimal(4, item.getItemValue());
            if (item.getParentItemId() != null) {
                ps.setInt(5, item.getParentItemId());
            } else {
                ps.setNull(5, Types.INTEGER);
            }

            int affectedRows = ps.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating StatementItem failed, no rows affected.");
            }
            try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    item.setId(generatedKeys.getInt(1));
                } else {
                    throw new SQLException("Creating StatementItem failed, no ID obtained.");
                }
            }
            LOGGER.info("StatementItem saved: {}", item);
            return item;
        } catch (SQLException e) {
            LOGGER.error("Error saving StatementItem {}: {}", item, e.getMessage());
            throw e;
        }
    }

    public void saveAll(List<StatementItem> items, int statementId) throws SQLException {
        // Пакетная вставка для производительности
        // Устанавливаем statementId для каждого элемента перед сохранением, если он еще не установлен
        for (StatementItem item : items) {
            item.setStatementId(statementId);
        }

        try (Connection connection = DatabaseConnector.getConnection();
             PreparedStatement ps = connection.prepareStatement(INSERT, Statement.RETURN_GENERATED_KEYS)) { // RETURN_GENERATED_KEYS здесь не так полезен для пакетной вставки, если ID не нужны сразу
            connection.setAutoCommit(false); // Начинаем транзакцию
            try {
                for (StatementItem item : items) {
                    ps.setInt(1, item.getStatementId());
                    ps.setString(2, item.getItemCode());
                    ps.setString(3, item.getItemName());
                    ps.setBigDecimal(4, item.getItemValue());
                    if (item.getParentItemId() != null) {
                        ps.setInt(5, item.getParentItemId());
                    } else {
                        ps.setNull(5, Types.INTEGER);
                    }
                    ps.addBatch();
                }
                ps.executeBatch();
                connection.commit(); // Фиксируем транзакцию
                LOGGER.info("Batch of {} StatementItems saved for statementId {}", items.size(), statementId);
            } catch (SQLException e) {
                connection.rollback(); // Откатываем транзакцию в случае ошибки
                LOGGER.error("Error batch saving StatementItems for statementId {}: {}", statementId, e.getMessage());
                throw e;
            } finally {
                connection.setAutoCommit(true); // Возвращаем автокоммит
            }
        }
    }


    @Override
    public void update(StatementItem item) throws SQLException {
        try (Connection connection = DatabaseConnector.getConnection();
             PreparedStatement ps = connection.prepareStatement(UPDATE)) {
            ps.setInt(1, item.getStatementId());
            ps.setString(2, item.getItemCode());
            ps.setString(3, item.getItemName());
            ps.setBigDecimal(4, item.getItemValue());
            if (item.getParentItemId() != null) {
                ps.setInt(5, item.getParentItemId());
            } else {
                ps.setNull(5, Types.INTEGER);
            }
            ps.setInt(6, item.getId());

            int affectedRows = ps.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Updating StatementItem failed, no rows affected for ID: " + item.getId());
            }
            LOGGER.info("StatementItem updated: {}", item);
        } catch (SQLException e) {
            LOGGER.error("Error updating StatementItem {}: {}", item, e.getMessage());
            throw e;
        }
    }

    @Override
    public void deleteById(Integer id) throws SQLException {
        try (Connection connection = DatabaseConnector.getConnection();
             PreparedStatement ps = connection.prepareStatement(DELETE_BY_ID)) {
            ps.setInt(1, id);
            int affectedRows = ps.executeUpdate();
            if (affectedRows == 0) {
                LOGGER.warn("No StatementItem found with ID {} to delete.", id);
            } else {
                LOGGER.info("StatementItem deleted with ID: {}", id);
            }
        } catch (SQLException e) {
            LOGGER.error("Error deleting StatementItem by id {}: {}", id, e.getMessage());
            throw e;
        }
    }

    public void deleteByStatementId(Integer statementId) throws SQLException {
        try (Connection connection = DatabaseConnector.getConnection();
             PreparedStatement ps = connection.prepareStatement(DELETE_BY_STATEMENT_ID)) {
            ps.setInt(1, statementId);
            int affectedRows = ps.executeUpdate();
            LOGGER.info("{} StatementItems deleted for statement_id: {}", affectedRows, statementId);
        } catch (SQLException e) {
            LOGGER.error("Error deleting StatementItems by statement_id {}: {}", statementId, e.getMessage());
            throw e;
        }
    }

    private StatementItem mapResultSetToStatementItem(ResultSet rs) throws SQLException {
        StatementItem item = new StatementItem();
        item.setId(rs.getInt("id"));
        item.setStatementId(rs.getInt("statement_id"));
        item.setItemCode(rs.getString("item_code"));
        item.setItemName(rs.getString("item_name"));
        item.setItemValue(rs.getBigDecimal("item_value"));
        item.setParentItemId(rs.getObject("parent_item_id", Integer.class));
        return item;
    }
}