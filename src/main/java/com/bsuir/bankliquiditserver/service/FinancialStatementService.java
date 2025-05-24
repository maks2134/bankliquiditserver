package com.bsuir.bankliquiditserver.service;

import com.bsuir.bankliquiditserver.dto.FinancialStatementDTO; // Создадим этот DTO
import com.bsuir.bankliquiditserver.exception.EntityNotFoundException;
import com.bsuir.bankliquiditserver.exception.ServiceException;
import com.bsuir.bankliquiditserver.exception.ValidationException;
import com.bsuir.bankliquiditserver.model.FinancialStatement;
import com.bsuir.bankliquiditserver.model.User; // Для указания, кто создал отчет

import java.time.LocalDate;
import java.util.List;

public interface FinancialStatementService {
    // Метод для создания или обновления отчета с его статьями
    // FinancialStatementDTO будет содержать информацию об отчете и список его статей
    FinancialStatement createFinancialStatement(FinancialStatementDTO statementDto, User createdByUser) throws ValidationException, ServiceException, EntityNotFoundException;

    FinancialStatement getFinancialStatementById(int statementId) throws EntityNotFoundException, ServiceException;

    // Получение отчета со всеми его статьями
    FinancialStatement getFinancialStatementWithItemsById(int statementId) throws EntityNotFoundException, ServiceException;

    List<FinancialStatement> getFinancialStatementsByBank(int bankId) throws ServiceException;

    // Получение списка отчетов для банка, но только метаданные (без статей)
    List<FinancialStatementDTO> getFinancialStatementOverviewsByBank(int bankId) throws ServiceException;

    // Получение отчета по банку, дате и типу (удобно для проверки уникальности)
    FinancialStatement getFinancialStatementByBankDateAndType(int bankId, LocalDate reportDate, String statementType) throws ServiceException, EntityNotFoundException;

    // Обновление только метаданных отчета (статьи обновляются через create/replace)
    void updateFinancialStatementMetadata(int statementId, FinancialStatementDTO statementDto, User updatedByUser) throws ValidationException, EntityNotFoundException, ServiceException;

    void deleteFinancialStatement(int statementId) throws EntityNotFoundException, ServiceException;

    // Возможно, методы для управления отдельными статьями, но чаще отчет загружается/сохраняется целиком
    // void addStatementItem(int statementId, StatementItemDTO itemDto) throws ...
    // void updateStatementItem(int itemId, StatementItemDTO itemDto) throws ...
    // void deleteStatementItem(int itemId) throws ...
}