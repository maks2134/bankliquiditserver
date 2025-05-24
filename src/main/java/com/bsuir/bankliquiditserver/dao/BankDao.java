package com.bsuir.bankliquiditserver.dao;

import com.bsuir.bankliquiditserver.db.DatabaseConnector;
import com.bsuir.bankliquiditserver.model.Bank;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class BankDao implements GenericDao<Bank, Integer> {
    private static final Logger LOGGER = LoggerFactory.getLogger(BankDao.class);

    private static final String SELECT_BANK_BY_ID = "SELECT id, name, registration_number, address FROM banks WHERE id = ?;";
    private static final String SELECT_ALL_BANKS = "SELECT id, name, registration_number, address FROM banks;";
    private static final String INSERT_BANK = "INSERT INTO banks (name, registration_number, address) VALUES (?, ?, ?);";
    private static final String UPDATE_BANK = "UPDATE banks SET name = ?, registration_number = ?, address = ? WHERE id = ?;";
    private static final String DELETE_BANK = "DELETE FROM banks WHERE id = ?;";
    private static final String SELECT_BANK_BY_NAME = "SELECT id, name, registration_number, address FROM banks WHERE name = ?;";
    private static final String SELECT_BANK_BY_REG_NUMBER = "SELECT id, name, registration_number, address FROM banks WHERE registration_number = ?;";

    @Override
    public Optional<Bank> findById(Integer id) throws SQLException {
        Bank bank = null;
        try (Connection connection = DatabaseConnector.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(SELECT_BANK_BY_ID)) {
            preparedStatement.setInt(1, id);
            ResultSet rs = preparedStatement.executeQuery();
            if (rs.next()) {
                bank = mapResultSetToBank(rs);
            }
        } catch (SQLException e) {
            LOGGER.error("Error finding bank by id {}: {}", id, e.getMessage());
            throw e;
        }
        return Optional.ofNullable(bank);
    }

    public Optional<Bank> findByName(String name) throws SQLException {
        Bank bank = null;
        try (Connection connection = DatabaseConnector.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(SELECT_BANK_BY_NAME)) {
            preparedStatement.setString(1, name);
            ResultSet rs = preparedStatement.executeQuery();
            if (rs.next()) {
                bank = mapResultSetToBank(rs);
            }
        } catch (SQLException e) {
            LOGGER.error("Error finding bank by name {}: {}", name, e.getMessage());
            throw e;
        }
        return Optional.ofNullable(bank);
    }

    @Override
    public List<Bank> findAll() throws SQLException {
        List<Bank> banks = new ArrayList<>();
        try (Connection connection = DatabaseConnector.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(SELECT_ALL_BANKS);
             ResultSet rs = preparedStatement.executeQuery()) {
            while (rs.next()) {
                banks.add(mapResultSetToBank(rs));
            }
        } catch (SQLException e) {
            LOGGER.error("Error finding all banks: {}", e.getMessage());
            throw e;
        }
        return banks;
    }

    @Override
    public Bank save(Bank bank) throws SQLException {
        try (Connection connection = DatabaseConnector.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(INSERT_BANK, Statement.RETURN_GENERATED_KEYS)) {
            preparedStatement.setString(1, bank.getName());
            preparedStatement.setString(2, bank.getRegistrationNumber());
            preparedStatement.setString(3, bank.getAddress());

            int affectedRows = preparedStatement.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating bank failed, no rows affected.");
            }
            try (ResultSet generatedKeys = preparedStatement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    bank.setId(generatedKeys.getInt(1));
                } else {
                    throw new SQLException("Creating bank failed, no ID obtained.");
                }
            }
            LOGGER.info("Bank saved: {}", bank);
            return bank;
        } catch (SQLException e) {
            LOGGER.error("Error saving bank {}: {}", bank, e.getMessage());
            throw e;
        }
    }

    @Override
    public void update(Bank bank) throws SQLException {
        try (Connection connection = DatabaseConnector.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(UPDATE_BANK)) {
            preparedStatement.setString(1, bank.getName());
            preparedStatement.setString(2, bank.getRegistrationNumber());
            preparedStatement.setString(3, bank.getAddress());
            preparedStatement.setInt(4, bank.getId());

            int affectedRows = preparedStatement.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Updating bank failed, no rows affected for ID: " + bank.getId());
            }
            LOGGER.info("Bank updated: {}", bank);
        } catch (SQLException e) {
            LOGGER.error("Error updating bank {}: {}", bank, e.getMessage());
            throw e;
        }
    }

    @Override
    public void deleteById(Integer id) throws SQLException {
        try (Connection connection = DatabaseConnector.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(DELETE_BANK)) {
            preparedStatement.setInt(1, id);
            int affectedRows = preparedStatement.executeUpdate();
            if (affectedRows == 0) {
                LOGGER.warn("No bank found with ID {} to delete.", id);
            } else {
                LOGGER.info("Bank deleted with ID: {}", id);
            }
        } catch (SQLException e) {
            LOGGER.error("Error deleting bank by id {}: {}", id, e.getMessage());
            throw e;
        }
    }

    private Bank mapResultSetToBank(ResultSet rs) throws SQLException {
        Bank bank = new Bank();
        bank.setId(rs.getInt("id"));
        bank.setName(rs.getString("name"));
        bank.setRegistrationNumber(rs.getString("registration_number"));
        bank.setAddress(rs.getString("address"));
        return bank;
    }

    public Optional<Bank> findByRegistrationNumber(String registrationNumber) throws SQLException {
        Bank bank = null;
        try (Connection connection = DatabaseConnector.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(SELECT_BANK_BY_REG_NUMBER)) {
            preparedStatement.setString(1, registrationNumber);
            ResultSet rs = preparedStatement.executeQuery();
            if (rs.next()) {
                bank = mapResultSetToBank(rs); // твой метод маппинга
            }
        } catch (SQLException e) {
            LOGGER.error("Error finding bank by registration number {}: {}", registrationNumber, e.getMessage());
            throw e;
        }
        return Optional.ofNullable(bank);
    }
}