package com.bsuir.bankliquiditserver.service.impl;

import com.bsuir.bankliquiditserver.dao.AuditLogDao;
import com.bsuir.bankliquiditserver.dao.UserDao; // Для обогащения логов информацией о пользователе
import com.bsuir.bankliquiditserver.exception.ServiceException;
import com.bsuir.bankliquiditserver.model.AuditLogEntry;
import com.bsuir.bankliquiditserver.model.User;
import com.bsuir.bankliquiditserver.service.AuditService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public class AuditServiceImpl implements AuditService {
    private static final Logger LOGGER = LoggerFactory.getLogger(AuditServiceImpl.class);

    private final AuditLogDao auditLogDao;
    private final UserDao userDao; // Опционально, для загрузки User объектов в AuditLogEntry

    public AuditServiceImpl(AuditLogDao auditLogDao, UserDao userDao) {
        this.auditLogDao = auditLogDao;
        this.userDao = userDao;
    }

    @Override
    public void logAction(Integer userId, String actionType, String details, String ipAddress, Boolean success) throws ServiceException {
        if (actionType == null || actionType.trim().isEmpty()) {
            LOGGER.warn("Attempted to log an action with empty actionType.");
            // Можно бросить ValidationException или просто не логировать
            return;
        }

        AuditLogEntry entry = new AuditLogEntry();
        entry.setUserId(userId);
        entry.setActionType(actionType.trim().toUpperCase()); // Нормализация
        entry.setDetails(details);
        entry.setIpAddress(ipAddress);
        entry.setTimestamp(LocalDateTime.now());
        entry.setSuccess(success);

        try {
            auditLogDao.save(entry);
            // Не логируем само логирование, чтобы избежать бесконечного цикла, если логирование ошибок тоже логируется :)
            // LOGGER.info("Audit action logged: UserID={}, Action={}, Success={}", userId, actionType, success);
        } catch (SQLException e) {
            LOGGER.error("Failed to log audit action (UserID={}, Action={}): {}", userId, actionType, e.getMessage(), e);
            // Не пробрасываем ServiceException наверх, т.к. ошибка логирования не должна останавливать основную операцию.
            // Но это зависит от требований. Если логирование критично, то нужно пробрасывать.
            // throw new ServiceException("Failed to write audit log.", e);
        }
    }

    @Override
    public void logUserAction(User user, String actionType, String details, String ipAddress, Boolean success) throws ServiceException {
        Integer userId = (user != null) ? user.getId() : null;
        logAction(userId, actionType, details, ipAddress, success);
    }

    @Override
    public List<AuditLogEntry> getAllAuditLogs() throws ServiceException {
        try {
            List<AuditLogEntry> logs = auditLogDao.findAll();
            // Обогащаем информацией о пользователе, если нужно
            enrichLogsWithUserDetails(logs);
            return logs;
        } catch (SQLException e) {
            LOGGER.error("Error fetching all audit logs: {}", e.getMessage(), e);
            throw new ServiceException("Failed to fetch audit logs.", e);
        }
    }

    @Override
    public List<AuditLogEntry> getAuditLogsByUser(int userId) throws ServiceException {
        try {
            // Предполагаем, что в AuditLogDao есть findByUserId
            List<AuditLogEntry> logs = auditLogDao.findByUserId(userId);
            enrichLogsWithUserDetails(logs); // Обогащаем, так как знаем ID пользователя
            return logs;
        } catch (SQLException e) {
            LOGGER.error("Error fetching audit logs for user {}: {}", userId, e.getMessage(), e);
            throw new ServiceException("Failed to fetch audit logs for user.", e);
        }
    }

    @Override
    public List<AuditLogEntry> getAuditLogsByActionType(String actionType) throws ServiceException {
        if (actionType == null || actionType.trim().isEmpty()) {
            return getAllAuditLogs(); // или пустой список, или ValidationException
        }
        try {
            // Предполагаем, что в AuditLogDao есть findByActionType
            List<AuditLogEntry> logs = auditLogDao.findByActionType(actionType.trim().toUpperCase());
            enrichLogsWithUserDetails(logs);
            return logs;
        } catch (SQLException e) {
            LOGGER.error("Error fetching audit logs by action type '{}': {}", actionType, e.getMessage(), e);
            throw new ServiceException("Failed to fetch audit logs by action type.", e);
        }
    }

    @Override
    public List<AuditLogEntry> getAuditLogsByPeriod(LocalDateTime startTime, LocalDateTime endTime) throws ServiceException {
        if (startTime == null || endTime == null || startTime.isAfter(endTime)) {
            throw new ServiceException("Invalid time period specified for audit logs.");
            // Или вернуть пустой список / ValidationException
        }
        try {
            // Это потребует нового метода в AuditLogDao, например, findByTimestampBetween(startTime, endTime)
            // Пока что отфильтруем из всех, что неэффективно для больших объемов.
            // return auditLogDao.findByTimestampBetween(startTime, endTime); // Идеальный вариант

            // Временное решение (фильтрация в памяти):
            List<AuditLogEntry> allLogs = auditLogDao.findAll();
            List<AuditLogEntry> filteredLogs = allLogs.stream()
                    .filter(log -> !log.getTimestamp().isBefore(startTime) && !log.getTimestamp().isAfter(endTime))
                    .collect(Collectors.toList());
            enrichLogsWithUserDetails(filteredLogs);
            return filteredLogs;
        } catch (SQLException e) {
            LOGGER.error("Error fetching audit logs by period: {}", e.getMessage(), e);
            throw new ServiceException("Failed to fetch audit logs by period.", e);
        }
    }

    private void enrichLogsWithUserDetails(List<AuditLogEntry> logs) {
        if (userDao == null) return; // Если UserDao не предоставлен, пропускаем обогащение

        for (AuditLogEntry log : logs) {
            if (log.getUserId() != null && log.getUser() == null) {
                try {
                    userDao.findById(log.getUserId()).ifPresent(log::setUser);
                } catch (SQLException e) {
                    LOGGER.warn("Could not enrich audit log entry {} with user details for userId {}: {}", log.getId(), log.getUserId(), e.getMessage());
                }
            }
        }
    }
}