package com.bsuir.bankliquiditserver;

import com.bsuir.bankliquiditserver.config.ConfigurationManager;
import com.bsuir.bankliquiditserver.controller.ClientHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServerApp {
    private static final Logger LOGGER = LoggerFactory.getLogger(ServerApp.class);
    private static final ConfigurationManager CONFIG = ConfigurationManager.getInstance();

    public static void main(String[] args) {
        int port = CONFIG.getIntProperty("server.port", 8080);
        int threadPoolSize = CONFIG.getIntProperty("server.threadPoolSize", 10);

        ExecutorService executorService = Executors.newFixedThreadPool(threadPoolSize);

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            LOGGER.info("Server started on port: {}", port);
            LOGGER.info("Waiting for client connections...");

            // Инициализация ServiceFactory (если в нем есть какая-то логика при старте)
            // ServiceFactory.initialize(); // Если бы он был не полностью статическим

            while (true) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    // Передаем сокет в ClientHandler
                    // ClientHandler будет использовать ServiceFactory для доступа к сервисам
                    executorService.submit(new ClientHandler(clientSocket));
                } catch (IOException e) {
                    LOGGER.error("Error accepting client connection: {}", e.getMessage(), e);
                    // Если ошибка критическая для серверного сокета, можно прервать цикл
                    if (serverSocket.isClosed()) {
                        LOGGER.error("Server socket closed, shutting down.");
                        break;
                    }
                }
            }
        } catch (IOException e) {
            LOGGER.error("Could not start server on port {}: {}", port, e.getMessage(), e);
        } finally {
            executorService.shutdown();
            LOGGER.info("Server shut down.");
        }
    }
}