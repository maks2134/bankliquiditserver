package com.bsuir.bankliquiditserver.service;

import com.bsuir.bankliquiditserver.exception.ServiceException;
import com.bsuir.bankliquiditserver.model.AuditLogEntry;
import com.bsuir.bankliquiditserver.model.User; // Для указания пользователя, если известен

import java.util.List;
import java.time.LocalDateTime;

public interface AuditService {

    /**
     * Записывает событие аудита.
     *
     * @param userId ID пользователя, инициировавшего действие (может быть null для системных событий или анонимных действий).
     * @param actionType Тип действия (например, "LOGIN_SUCCESS", "CREATE_USER", "CALCULATE_LIQUIDITY").
     * @param details Дополнительные детали о событии (например, ID созданной сущности, параметры запроса).
     * @param ipAddress IP-адрес клиента.
     * @param success Результат операции (true - успешно, false - неудача, null - неприменимо).
     * @throws ServiceException если произошла ошибка при записи лога.
     */
    void logAction(Integer userId, String actionType, String details, String ipAddress, Boolean success) throws ServiceException;

    /**
     * Упрощенный метод для логирования действий аутентифицированного пользователя.
     */
    void logUserAction(User user, String actionType, String details, String ipAddress, Boolean success) throws ServiceException;


    /**
     * Получает все записи аудита.
     * @return список записей аудита.
     * @throws ServiceException если произошла ошибка при чтении логов.
     */
    List<AuditLogEntry> getAllAuditLogs() throws ServiceException;

    /**
     * Получает записи аудита для конкретного пользователя.
     * @param userId ID пользователя.
     * @return список записей аудита для пользователя.
     * @throws ServiceException если произошла ошибка при чтении логов.
     */
    List<AuditLogEntry> getAuditLogsByUser(int userId) throws ServiceException;

    /**
     * Получает записи аудита по типу действия.
     * @param actionType тип действия.
     * @return список записей аудита.
     * @throws ServiceException если произошла ошибка при чтении логов.
     */
    List<AuditLogEntry> getAuditLogsByActionType(String actionType) throws ServiceException;

    /**
     * Получает записи аудита за определенный период.
     * @param startTime начальное время периода.
     * @param endTime конечное время периода.
     * @return список записей аудита.
     * @throws ServiceException если произошла ошибка при чтении логов.
     */
    List<AuditLogEntry> getAuditLogsByPeriod(LocalDateTime startTime, LocalDateTime endTime) throws ServiceException;

    // Возможно, понадобится метод для очистки старых логов (с осторожностью!)
    // void purgeOldAuditLogs(LocalDateTime beforeDate) throws ServiceException;
}