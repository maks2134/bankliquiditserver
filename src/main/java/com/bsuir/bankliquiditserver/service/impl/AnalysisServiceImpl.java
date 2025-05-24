package com.bsuir.bankliquiditserver.service.impl;

import com.bsuir.bankliquiditserver.dao.AnalysisReportDao;
import com.bsuir.bankliquiditserver.dao.BankDao;
import com.bsuir.bankliquiditserver.dto.LiquidityAnalysisResultDTO;
import com.bsuir.bankliquiditserver.dto.SolvencyAnalysisResultDTO;
import com.bsuir.bankliquiditserver.exception.EntityNotFoundException;
import com.bsuir.bankliquiditserver.exception.ServiceException;
import com.bsuir.bankliquiditserver.model.*; // Bank, FinancialStatement, StatementItem, User, AnalysisReport
import com.bsuir.bankliquiditserver.service.AnalysisService;
import com.bsuir.bankliquiditserver.service.AuditService;
import com.bsuir.bankliquiditserver.service.FinancialStatementService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public class AnalysisServiceImpl implements AnalysisService {
    private static final Logger LOGGER = LoggerFactory.getLogger(AnalysisServiceImpl.class);
    private static final int DEFAULT_SCALE = 4; // Точность для коэффициентов

    private final FinancialStatementService financialStatementService;
    private final AnalysisReportDao analysisReportDao;
    private final BankDao bankDao;
    private final AuditService auditService;
    private final ObjectMapper objectMapper; // Для сериализации reportData в JSON

    // === Константы для кодов или имен статей баланса ===
    // !!! ВАЖНО: Эти значения нужно будет адаптировать под твою структуру фин. отчетов !!!
    // Это только примеры!
    private static final List<String> CURRENT_ASSETS_CODES = Arrays.asList("CA01", "CA02", "CA03", "DEBT_S", "INV01"); // ДС, Краткоср. фин. влож., Деб. задолж., Запасы
    private static final List<String> INVENTORY_CODES = Arrays.asList("INV01", "INV02"); // Запасы
    private static final List<String> CASH_AND_EQUIVALENTS_CODES = Arrays.asList("CA01", "CA02"); // ДС и эквиваленты
    private static final List<String> CURRENT_LIABILITIES_CODES = Arrays.asList("CL01", "CL02", "CREDIT_S"); // Краткоср. кредиты, Кред. задолж.
    private static final List<String> TOTAL_ASSETS_CODES = Arrays.asList("TOTAL_ASSETS_BALANCE"); // Валюта баланса (итог актива)
    private static final List<String> TOTAL_EQUITY_CODES = Arrays.asList("EQUITY01", "EQUITY02"); // Собственный капитал
    private static final List<String> TOTAL_LIABILITIES_CODES = Arrays.asList("CL01", "CL02", "LL01"); // Все обязательства (краткосрочные + долгосрочные)


    public AnalysisServiceImpl(FinancialStatementService financialStatementService,
                               AnalysisReportDao analysisReportDao,
                               BankDao bankDao,
                               AuditService auditService) {
        this.financialStatementService = financialStatementService;
        this.analysisReportDao = analysisReportDao;
        this.bankDao = bankDao;
        this.auditService = auditService;
        this.objectMapper = new ObjectMapper().findAndRegisterModules(); // Для Java 8 Date/Time
    }

    @Override
    public LiquidityAnalysisResultDTO calculateLiquidity(int bankId, LocalDate reportDate, User currentUser, String ipAddress)
            throws EntityNotFoundException, ServiceException {
        FinancialStatement statement = getRelevantFinancialStatement(bankId, reportDate, "BALANCE_SHEET");
        Bank bank = statement.getBank(); // Банк уже должен быть загружен в statement
        if (bank == null) { // Дополнительная проверка
            try {
                bank = bankDao.findById(bankId).orElseThrow(() -> new EntityNotFoundException("Bank", bankId));
            } catch (SQLException e) { throw new ServiceException("Failed to load bank details", e); }
        }

        List<StatementItem> items = statement.getItems();
        if (items == null || items.isEmpty()) {
            throw new ServiceException("Financial statement for bank " + bank.getName() + " on " + statement.getReportDate() + " contains no items.");
        }

        BigDecimal currentAssets = sumItemsByCodes(items, CURRENT_ASSETS_CODES);
        BigDecimal inventories = sumItemsByCodes(items, INVENTORY_CODES);
        BigDecimal cashAndEquivalents = sumItemsByCodes(items, CASH_AND_EQUIVALENTS_CODES);
        BigDecimal currentLiabilities = sumItemsByCodes(items, CURRENT_LIABILITIES_CODES);

        LiquidityAnalysisResultDTO result = new LiquidityAnalysisResultDTO();
        result.setBankId(bank.getId());
        result.setBankName(bank.getName());
        result.setReportDate(statement.getReportDate());

        // Current Ratio = Current Assets / Current Liabilities
        if (currentLiabilities.compareTo(BigDecimal.ZERO) != 0) {
            result.setCurrentRatio(currentAssets.divide(currentLiabilities, DEFAULT_SCALE, RoundingMode.HALF_UP));
        } else {
            result.setCurrentRatio(null); // или BigDecimal.ZERO, или большое число в зависимости от политики
            LOGGER.warn("Current Liabilities are zero for bank {}, date {}. Current Ratio cannot be calculated meaningfully.", bankId, statement.getReportDate());
        }

        // Quick Ratio = (Current Assets - Inventories) / Current Liabilities
        if (currentLiabilities.compareTo(BigDecimal.ZERO) != 0) {
            BigDecimal quickAssets = currentAssets.subtract(inventories);
            result.setQuickRatio(quickAssets.divide(currentLiabilities, DEFAULT_SCALE, RoundingMode.HALF_UP));
        } else {
            result.setQuickRatio(null);
        }

        // Cash Ratio = Cash & Equivalents / Current Liabilities
        if (currentLiabilities.compareTo(BigDecimal.ZERO) != 0) {
            result.setCashRatio(cashAndEquivalents.divide(currentLiabilities, DEFAULT_SCALE, RoundingMode.HALF_UP));
        } else {
            result.setCashRatio(null);
        }

        // Интерпретации (пример)
        interpretLiquidityRatios(result);

        auditService.logUserAction(currentUser, "CALCULATE_LIQUIDITY",
                "Liquidity calculated for bank: " + bank.getName() + ", report date: " + statement.getReportDate(), ipAddress, true);

        return result;
    }


    @Override
    public SolvencyAnalysisResultDTO calculateSolvency(int bankId, LocalDate reportDate, User currentUser, String ipAddress)
            throws EntityNotFoundException, ServiceException {
        FinancialStatement statement = getRelevantFinancialStatement(bankId, reportDate, "BALANCE_SHEET");
        Bank bank = statement.getBank();
        if (bank == null) {
            try {
                bank = bankDao.findById(bankId).orElseThrow(() -> new EntityNotFoundException("Bank", bankId));
            } catch (SQLException e) { throw new ServiceException("Failed to load bank details", e); }
        }


        List<StatementItem> items = statement.getItems();
        if (items == null || items.isEmpty()) {
            throw new ServiceException("Financial statement for bank " + bank.getName() + " on " + statement.getReportDate() + " contains no items.");
        }

        BigDecimal totalAssets = sumItemsByCodes(items, TOTAL_ASSETS_CODES);
        BigDecimal totalEquity = sumItemsByCodes(items, TOTAL_EQUITY_CODES);
        BigDecimal totalLiabilities = sumItemsByCodes(items, TOTAL_LIABILITIES_CODES); // Или (Total Assets - Total Equity)

        SolvencyAnalysisResultDTO result = new SolvencyAnalysisResultDTO();
        result.setBankId(bank.getId());
        result.setBankName(bank.getName());
        result.setReportDate(statement.getReportDate());

        // Equity Ratio = Total Equity / Total Assets
        if (totalAssets.compareTo(BigDecimal.ZERO) != 0) {
            result.setEquityRatio(totalEquity.divide(totalAssets, DEFAULT_SCALE, RoundingMode.HALF_UP));
        } else {
            result.setEquityRatio(null);
            LOGGER.warn("Total Assets are zero for bank {}, date {}. Equity Ratio cannot be calculated.", bankId, statement.getReportDate());
        }

        // Debt to Equity Ratio = Total Liabilities / Total Equity
        if (totalEquity.compareTo(BigDecimal.ZERO) != 0) {
            result.setDebtToEquityRatio(totalLiabilities.divide(totalEquity, DEFAULT_SCALE, RoundingMode.HALF_UP));
        } else {
            result.setDebtToEquityRatio(null);
            LOGGER.warn("Total Equity is zero for bank {}, date {}. Debt to Equity Ratio cannot be calculated meaningfully.", bankId, statement.getReportDate());
        }

        interpretSolvencyRatios(result);

        auditService.logUserAction(currentUser, "CALCULATE_SOLVENCY",
                "Solvency calculated for bank: " + bank.getName() + ", report date: " + statement.getReportDate(), ipAddress, true);
        return result;
    }

    private FinancialStatement getRelevantFinancialStatement(int bankId, LocalDate reportDate, String statementType)
            throws EntityNotFoundException, ServiceException {
        FinancialStatement statement;
        if (reportDate != null) {
            statement = financialStatementService.getFinancialStatementByBankDateAndType(bankId, reportDate, statementType);
        } else {
            // Логика получения последнего отчета для банка данного типа
            List<FinancialStatement> statements = financialStatementService.getFinancialStatementsByBank(bankId);
            statement = statements.stream()
                    .filter(s -> statementType.equals(s.getStatementType()))
                    .max(java.util.Comparator.comparing(FinancialStatement::getReportDate))
                    .orElseThrow(() -> new EntityNotFoundException(
                            String.format("No '%s' found for bank ID %d.", statementType, bankId)));
        }
        // Убедимся, что статьи загружены
        return financialStatementService.getFinancialStatementWithItemsById(statement.getId());
    }


    private BigDecimal sumItemsByCodes(List<StatementItem> items, List<String> targetCodes) {
        return items.stream()
                .filter(item -> targetCodes.contains(item.getItemCode()) || targetCodes.contains(item.getItemName())) // Поиск по коду или имени
                .map(StatementItem::getItemValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // Методы для интерпретации (можно вынести в отдельный класс/сервис)
    private void interpretLiquidityRatios(LiquidityAnalysisResultDTO result) {
        if (result.getCurrentRatio() != null) {
            if (result.getCurrentRatio().compareTo(BigDecimal.valueOf(2)) >= 0) result.setCurrentRatioInterpretation("Высокий уровень (норма > 1.5-2)");
            else if (result.getCurrentRatio().compareTo(BigDecimal.valueOf(1)) >= 0) result.setCurrentRatioInterpretation("Удовлетворительный уровень");
            else result.setCurrentRatioInterpretation("Низкий уровень (риск неплатежеспособности)");
        }
        // Аналогично для других коэффициентов
    }

    private void interpretSolvencyRatios(SolvencyAnalysisResultDTO result) {
        if (result.getEquityRatio() != null) {
            if (result.getEquityRatio().compareTo(BigDecimal.valueOf(0.5)) >= 0) result.setEquityRatioInterpretation("Высокая финансовая независимость (норма > 0.4-0.6)");
            else if (result.getEquityRatio().compareTo(BigDecimal.valueOf(0.3)) >= 0) result.setEquityRatioInterpretation("Удовлетворительная финансовая независимость");
            else result.setEquityRatioInterpretation("Низкая финансовая независимость, высокая зависимость от заемных средств");
        }
    }

    @Override
    public AnalysisReport saveLiquidityAnalysisReport(LiquidityAnalysisResultDTO resultDTO, User analyzedBy) throws ServiceException {
        AnalysisReport report = new AnalysisReport();
        report.setBankId(resultDTO.getBankId());
        // Загружаем Bank объект, если нужно его сохранить в AnalysisReport (для поля Bank bank)
        try {
            bankDao.findById(resultDTO.getBankId()).ifPresent(report::setBank);
        } catch (SQLException e) {
            LOGGER.warn("Could not set Bank object for analysis report of bankId {}: {}", resultDTO.getBankId(), e.getMessage());
        }

        report.setReportType("LIQUIDITY_ANALYSIS");
        report.setAnalysisDate(LocalDateTime.now());
        report.setAnalyzedByUserId(analyzedBy.getId());
        report.setAnalyzedByUser(analyzedBy);

        report.setCurrentRatio(resultDTO.getCurrentRatio());
        report.setQuickRatio(resultDTO.getQuickRatio());
        report.setCashRatio(resultDTO.getCashRatio());

        // Сериализуем DTO в JSON для поля report_data
        try {
            report.setReportData(objectMapper.writeValueAsString(resultDTO));
        } catch (JsonProcessingException e) {
            LOGGER.error("Error serializing LiquidityAnalysisResultDTO to JSON for bankId {}: {}", resultDTO.getBankId(), e.getMessage(), e);
            throw new ServiceException("Failed to serialize analysis data.", e);
        }

        try {
            AnalysisReport savedReport = analysisReportDao.save(report);
            auditService.logUserAction(analyzedBy, "SAVE_LIQUIDITY_REPORT", "Liquidity report saved, ID: " + savedReport.getId(), null, true);
            return savedReport;
        } catch (SQLException e) {
            LOGGER.error("Error saving liquidity analysis report for bankId {}: {}", resultDTO.getBankId(), e.getMessage(), e);
            throw new ServiceException("Failed to save liquidity analysis report.", e);
        }
    }

    @Override
    public AnalysisReport saveSolvencyAnalysisReport(SolvencyAnalysisResultDTO resultDTO, User analyzedBy) throws ServiceException {
        AnalysisReport report = new AnalysisReport();
        report.setBankId(resultDTO.getBankId());
        try {
            bankDao.findById(resultDTO.getBankId()).ifPresent(report::setBank);
        } catch (SQLException e) {
            LOGGER.warn("Could not set Bank object for analysis report of bankId {}: {}", resultDTO.getBankId(), e.getMessage());
        }

        report.setReportType("SOLVENCY_ANALYSIS");
        report.setAnalysisDate(LocalDateTime.now());
        report.setAnalyzedByUserId(analyzedBy.getId());
        report.setAnalyzedByUser(analyzedBy);

        report.setDebtToEquityRatio(resultDTO.getDebtToEquityRatio());
        // Другие поля solvency, если они есть в AnalysisReport модели
        // report.setEquityRatio(resultDTO.getEquityRatio()); // Если добавили поле в AnalysisReport.java

        try {
            report.setReportData(objectMapper.writeValueAsString(resultDTO));
        } catch (JsonProcessingException e) {
            LOGGER.error("Error serializing SolvencyAnalysisResultDTO to JSON for bankId {}: {}", resultDTO.getBankId(), e.getMessage(), e);
            throw new ServiceException("Failed to serialize analysis data.", e);
        }

        try {
            AnalysisReport savedReport = analysisReportDao.save(report);
            auditService.logUserAction(analyzedBy, "SAVE_SOLVENCY_REPORT", "Solvency report saved, ID: " + savedReport.getId(), null, true);
            return savedReport;
        } catch (SQLException e) {
            LOGGER.error("Error saving solvency analysis report for bankId {}: {}", resultDTO.getBankId(), e.getMessage(), e);
            throw new ServiceException("Failed to save solvency analysis report.", e);
        }
    }


    @Override
    public AnalysisReport getAnalysisReportById(int reportId) throws EntityNotFoundException, ServiceException {
        try {
            AnalysisReport report = analysisReportDao.findById(reportId)
                    .orElseThrow(() -> new EntityNotFoundException("AnalysisReport", reportId));
            // Дозагрузка связанных сущностей Bank и User, если это не сделано в DAO
            if (report.getBank() == null && report.getBankId() > 0) {
                bankDao.findById(report.getBankId()).ifPresent(report::setBank);
            }
            // userDao не объявлен в этом классе, нужно будет его добавить, если хотим загружать User
            // if (report.getAnalyzedByUser() == null && report.getAnalyzedByUserId() > 0) {
            //     userDao.findById(report.getAnalyzedByUserId()).ifPresent(report::setAnalyzedByUser);
            // }
            return report;
        } catch (SQLException e) {
            LOGGER.error("Error fetching analysis report by id {}: {}", reportId, e.getMessage(), e);
            throw new ServiceException("Failed to fetch analysis report.", e);
        }
    }

    @Override
    public List<AnalysisReport> getAnalysisReportsByBank(int bankId) throws ServiceException {
        try {
            List<AnalysisReport> reports = analysisReportDao.findByBankId(bankId); // Предполагается, что метод есть в DAO
            // Дозагрузка Bank и User для каждого отчета
            Bank bank = null; // Оптимизация
            if (!reports.isEmpty()) {
                bank = bankDao.findById(reports.get(0).getBankId()).orElse(null);
            }
            final Bank finalBank = bank;
            reports.forEach(r -> {
                if (r.getBank() == null && finalBank != null && finalBank.getId() == r.getBankId()) {
                    r.setBank(finalBank);
                }
                // Загрузка User, если нужно и userDao доступен
            });
            return reports;
        } catch (SQLException e) {
            LOGGER.error("Error fetching analysis reports for bank id {}: {}", bankId, e.getMessage(), e);
            throw new ServiceException("Failed to fetch analysis reports.", e);
        }
    }

    @Override
    public void deleteAnalysisReport(int reportId, User currentUser, String ipAddress) throws EntityNotFoundException, ServiceException {
        try {
            if (!analysisReportDao.findById(reportId).isPresent()){
                throw new EntityNotFoundException("AnalysisReport", reportId);
            }
            analysisReportDao.deleteById(reportId);
            auditService.logUserAction(currentUser, "DELETE_ANALYSIS_REPORT", "Analysis report deleted, ID: " + reportId, ipAddress, true);
            LOGGER.info("AnalysisReport deleted with ID: {}", reportId);
        } catch (SQLException e) {
            LOGGER.error("Error deleting analysis report with ID {}: {}", reportId, e.getMessage(), e);
            throw new ServiceException("Failed to delete analysis report.", e);
        }
    }
}