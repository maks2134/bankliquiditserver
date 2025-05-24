package com.bsuir.bankliquiditserver.service;

import com.bsuir.bankliquiditserver.dto.LiquidityAnalysisResultDTO;
import com.bsuir.bankliquiditserver.dto.SolvencyAnalysisResultDTO;
import com.bsuir.bankliquiditserver.exception.EntityNotFoundException;
import com.bsuir.bankliquiditserver.exception.ServiceException;
import com.bsuir.bankliquiditserver.model.AnalysisReport;
import com.bsuir.bankliquiditserver.model.User; // Для указания, кто выполнил анализ

import java.time.LocalDate;
import java.util.List;

public interface AnalysisService {

    /**
     * Рассчитывает показатели ликвидности для банка на основе последнего доступного балансового отчета.
     * @param bankId ID банка.
     * @param reportDate Дата отчета, для которого проводить анализ. Если null, берется последний.
     * @param currentUser Пользователь, выполняющий анализ (для аудита и сохранения отчета).
     * @param ipAddress IP-адрес клиента (для аудита).
     * @return DTO с результатами анализа ликвидности.
     * @throws EntityNotFoundException если банк или подходящий финансовый отчет не найдены.
     * @throws ServiceException если произошла ошибка в процессе анализа.
     */
    LiquidityAnalysisResultDTO calculateLiquidity(int bankId, LocalDate reportDate, User currentUser, String ipAddress) throws EntityNotFoundException, ServiceException;

    /**
     * Рассчитывает показатели платежеспособности для банка на основе последнего доступного балансового отчета.
     * @param bankId ID банка.
     * @param reportDate Дата отчета, для которого проводить анализ. Если null, берется последний.
     * @param currentUser Пользователь, выполняющий анализ.
     * @param ipAddress IP-адрес клиента.
     * @return DTO с результатами анализа платежеспособности.
     * @throws EntityNotFoundException если банк или подходящий финансовый отчет не найдены.
     * @throws ServiceException если произошла ошибка в процессе анализа.
     */
    SolvencyAnalysisResultDTO calculateSolvency(int bankId, LocalDate reportDate, User currentUser, String ipAddress) throws EntityNotFoundException, ServiceException;

    /**
     * Сохраняет результаты анализа ликвидности.
     * @param resultDTO DTO с результатами.
     * @param analyzedBy Пользователь, выполнивший анализ.
     * @return Сохраненный объект AnalysisReport.
     * @throws ServiceException при ошибке сохранения.
     */
    AnalysisReport saveLiquidityAnalysisReport(LiquidityAnalysisResultDTO resultDTO, User analyzedBy) throws ServiceException;

    /**
     * Сохраняет результаты анализа платежеспособности.
     * @param resultDTO DTO с результатами.
     * @param analyzedBy Пользователь, выполнивший анализ.
     * @return Сохраненный объект AnalysisReport.
     * @throws ServiceException при ошибке сохранения.
     */
    AnalysisReport saveSolvencyAnalysisReport(SolvencyAnalysisResultDTO resultDTO, User analyzedBy) throws ServiceException;

    /**
     * Получает сохраненный отчет по анализу по его ID.
     * @param reportId ID отчета.
     * @return Объект AnalysisReport.
     * @throws EntityNotFoundException если отчет не найден.
     * @throws ServiceException при ошибке получения.
     */
    AnalysisReport getAnalysisReportById(int reportId) throws EntityNotFoundException, ServiceException;

    /**
     * Получает все сохраненные отчеты по анализу для указанного банка.
     * @param bankId ID банка.
     * @return Список отчетов AnalysisReport.
     * @throws ServiceException при ошибке получения.
     */
    List<AnalysisReport> getAnalysisReportsByBank(int bankId) throws ServiceException;

    /**
     * Удаляет сохраненный отчет по анализу.
     * @param reportId ID отчета.
     * @param currentUser Пользователь, выполняющий удаление (для аудита).
     * @param ipAddress IP-адрес клиента.
     * @throws EntityNotFoundException если отчет не найден.
     * @throws ServiceException при ошибке удаления.
     */
    void deleteAnalysisReport(int reportId, User currentUser, String ipAddress) throws EntityNotFoundException, ServiceException;
}