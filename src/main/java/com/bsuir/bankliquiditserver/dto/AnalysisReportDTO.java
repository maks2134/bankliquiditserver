package com.bsuir.bankliquiditserver.dto;

import com.bsuir.bankliquiditserver.model.AnalysisReport;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger; // Добавлен импорт Logger

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

public class AnalysisReportDTO implements Serializable {
    private static final long serialVersionUID = 305L;
    private static final Logger LOGGER = LoggerFactory.getLogger(AnalysisReportDTO.class); // Логгер

    private int id;
    private int bankId;
    private String bankName;
    private String reportType;
    private LocalDateTime analysisDate;
    private int analyzedByUserId;
    private String analyzedByUsername;

    // Основные показатели (могут быть и другие)
    private BigDecimal currentRatio;
    private BigDecimal quickRatio;
    private BigDecimal cashRatio;
    private BigDecimal debtToEquityRatio;
    private BigDecimal totalDebtToTotalAssetsRatio; // Добавлено из SQL

    // Поле для хранения полного JSON из report_data, если он нужен клиенту "как есть"
    private JsonNode reportDataJson;
    // Или можно десериализовать reportData в конкретный объект, если его структура известна
    // private SpecificReportDataDetails specificDetails;

    public AnalysisReportDTO() {}

    // Геттеры
    public int getId() { return id; }
    public int getBankId() { return bankId; }
    public String getBankName() { return bankName; }
    public String getReportType() { return reportType; }
    public LocalDateTime getAnalysisDate() { return analysisDate; }
    public int getAnalyzedByUserId() { return analyzedByUserId; }
    public String getAnalyzedByUsername() { return analyzedByUsername; }
    public BigDecimal getCurrentRatio() { return currentRatio; }
    public BigDecimal getQuickRatio() { return quickRatio; }
    public BigDecimal getCashRatio() { return cashRatio; }
    public BigDecimal getDebtToEquityRatio() { return debtToEquityRatio; }
    public BigDecimal getTotalDebtToTotalAssetsRatio() { return totalDebtToTotalAssetsRatio; }
    public JsonNode getReportDataJson() { return reportDataJson; }

    // Сеттеры
    public void setId(int id) { this.id = id; }
    public void setBankId(int bankId) { this.bankId = bankId; }
    public void setBankName(String bankName) { this.bankName = bankName; }
    public void setReportType(String reportType) { this.reportType = reportType; }
    public void setAnalysisDate(LocalDateTime analysisDate) { this.analysisDate = analysisDate; }
    public void setAnalyzedByUserId(int analyzedByUserId) { this.analyzedByUserId = analyzedByUserId; }
    public void setAnalyzedByUsername(String analyzedByUsername) { this.analyzedByUsername = analyzedByUsername; }
    public void setCurrentRatio(BigDecimal currentRatio) { this.currentRatio = currentRatio; }
    public void setQuickRatio(BigDecimal quickRatio) { this.quickRatio = quickRatio; }
    public void setCashRatio(BigDecimal cashRatio) { this.cashRatio = cashRatio; }
    public void setDebtToEquityRatio(BigDecimal debtToEquityRatio) { this.debtToEquityRatio = debtToEquityRatio; }
    public void setTotalDebtToTotalAssetsRatio(BigDecimal totalDebtToTotalAssetsRatio) { this.totalDebtToTotalAssetsRatio = totalDebtToTotalAssetsRatio; }
    public void setReportDataJson(JsonNode reportDataJson) { this.reportDataJson = reportDataJson; }


    public static AnalysisReportDTO fromAnalysisReport(AnalysisReport report) {
        if (report == null) return null;
        AnalysisReportDTO dto = new AnalysisReportDTO();
        dto.setId(report.getId());
        dto.setBankId(report.getBankId());
        if (report.getBank() != null) {
            dto.setBankName(report.getBank().getName());
        }
        dto.setReportType(report.getReportType());
        dto.setAnalysisDate(report.getAnalysisDate());
        dto.setAnalyzedByUserId(report.getAnalyzedByUserId());
        if (report.getAnalyzedByUser() != null) {
            dto.setAnalyzedByUsername(report.getAnalyzedByUser().getUsername());
        }

        // Копируем основные числовые показатели
        dto.setCurrentRatio(report.getCurrentRatio());
        dto.setQuickRatio(report.getQuickRatio());
        dto.setCashRatio(report.getCashRatio());
        dto.setDebtToEquityRatio(report.getDebtToEquityRatio());
        dto.setTotalDebtToTotalAssetsRatio(report.getTotalDebtToTotalAssetsRatio());


        if (report.getReportData() != null && !report.getReportData().isEmpty()) {
            try {
                ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
                dto.setReportDataJson(mapper.readTree(report.getReportData()));
            } catch (JsonProcessingException e) {
                LOGGER.warn("Could not parse reportData JSON for report ID {}: {}. Raw data: '{}'",
                        report.getId(), e.getMessage(), report.getReportData());
                // В случае ошибки можно, например, создать JSON объект с полем error
                // dto.setReportDataJson(new ObjectMapper().createObjectNode().put("error", "Failed to parse original JSON data"));
            }
        }
        return dto;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AnalysisReportDTO that = (AnalysisReportDTO) o;
        return id == that.id &&
                bankId == that.bankId &&
                analyzedByUserId == that.analyzedByUserId &&
                Objects.equals(reportType, that.reportType) &&
                Objects.equals(analysisDate, that.analysisDate); // Сравниваем основные поля, не reportDataJson
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, bankId, reportType, analysisDate, analyzedByUserId);
    }

    @Override
    public String toString() {
        return "AnalysisReportDTO{" +
                "id=" + id +
                ", bankId=" + bankId +
                ", reportType='" + reportType + '\'' +
                ", analysisDate=" + analysisDate +
                ", reportDataJsonPresent=" + (reportDataJson != null && !reportDataJson.isNull() && !reportDataJson.isMissingNode()) +
                '}';
    }
}