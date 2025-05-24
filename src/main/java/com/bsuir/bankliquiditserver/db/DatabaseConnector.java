package com.bsuir.bankliquiditserver.db;

import com.bsuir.bankliquiditserver.config.ConfigurationManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnector {
    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseConnector.class);
    private static final ConfigurationManager CONFIG = ConfigurationManager.getInstance();
    private static final String DB_URL = CONFIG.getProperty("db.url");
    private static final String DB_USERNAME = CONFIG.getProperty("db.username");
    private static final String DB_PASSWORD = CONFIG.getProperty("db.password");

    // Статическая инициализация драйвера
    static {
        try {
            Class.forName("org.postgresql.Driver");
            LOGGER.info("PostgreSQL JDBC Driver registered successfully.");
        } catch (ClassNotFoundException e) {
            LOGGER.error("PostgreSQL JDBC Driver not found!", e);
            throw new RuntimeException("PostgreSQL JDBC Driver not found!", e);
        }
    }

    public static Connection getConnection() throws SQLException {
        Connection connection = DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD);
        LOGGER.debug("Database connection established: {}", connection);
        return connection;
    }

    // Пример использования в try-with-resources:
    // try (Connection conn = DatabaseConnector.getConnection()) {
    //     // ... ваш JDBC код ...
    // } catch (SQLException e) {
    //     LOGGER.error("Database error", e);
    // }
}