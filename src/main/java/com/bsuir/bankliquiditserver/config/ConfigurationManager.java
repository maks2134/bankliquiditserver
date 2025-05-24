package com.bsuir.bankliquiditserver.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

// Паттерн Singleton для единственного экземпляра конфигурации
public class ConfigurationManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigurationManager.class);
    private static ConfigurationManager instance;
    private final Properties properties;

    private ConfigurationManager() {
        properties = new Properties();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("server.properties")) {
            if (input == null) {
                LOGGER.error("Sorry, unable to find server.properties");
                // Можно бросить RuntimeException, если конфигурация критична
                throw new RuntimeException("Configuration file 'server.properties' not found in classpath.");
            }
            properties.load(input);
            LOGGER.info("Server configuration loaded successfully.");
        } catch (IOException ex) {
            LOGGER.error("Error loading server configuration", ex);
            // Аналогично, можно бросить исключение
            throw new RuntimeException("Error loading server configuration", ex);
        }
    }

    public static synchronized ConfigurationManager getInstance() {
        if (instance == null) {
            instance = new ConfigurationManager();
        }
        return instance;
    }

    public String getProperty(String key) {
        return properties.getProperty(key);
    }

    public int getIntProperty(String key, int defaultValue) {
        String value = properties.getProperty(key);
        if (value != null) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                LOGGER.warn("Invalid integer value for key '{}': {}. Using default: {}", key, value, defaultValue);
                return defaultValue;
            }
        }
        return defaultValue;
    }
}