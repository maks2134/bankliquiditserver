package com.bsuir.bankliquiditserver.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

public class AnalysisReport implements Serializable {
    private static final long serialVersionUID = 6L;

    private int id;
    private int bankId;
    private Bank bank; // Объект банка
    private String reportType; // 'LIQUIDITY_ANALYSIS', 'SOLVENCY_ANALYSIS'
    private LocalDateTime analysisDate;
    private int analyzedByUserId;
    private User analyzedByUser; // Объект пользователя

    // Показатели можно хранить здесь или в reportData
    private BigDecimal currentRatio;
    private BigDecimal quickRatio;
    private BigDecimal cashRatio;
    private BigDecimal debtToEquityRatio;
    private BigDecimal totalDebtToTotalAssetsRatio;

    private String reportData; // JSON строка для хранения структурированных данных

    // Конструкторы, геттеры/сеттеры, equals/hashCode/toString
    public AnalysisReport() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getBankId() {
        return bankId;
    }

    public void setBankId(int bankId) {
        this.bankId = bankId;
    }

    public Bank getBank() {
        return bank;
    }

    public void setBank(Bank bank) {
        this.bank = bank;
        if (bank != null) {
            this.bankId = bank.getId();
        }
    }

    public String getReportType() {
        return reportType;
    }

    public void setReportType(String reportType) {
        this.reportType = reportType;
    }

    public LocalDateTime getAnalysisDate() {
        return analysisDate;
    }

    public void setAnalysisDate(LocalDateTime analysisDate) {
        this.analysisDate = analysisDate;
    }

    public int getAnalyzedByUserId() {
        return analyzedByUserId;
    }

    public void setAnalyzedByUserId(int analyzedByUserId) {
        this.analyzedByUserId = analyzedByUserId;
    }

    public User getAnalyzedByUser() {
        return analyzedByUser;
    }

    public void setAnalyzedByUser(User analyzedByUser) {
        this.analyzedByUser = analyzedByUser;
        if (analyzedByUser != null) {
            this.analyzedByUserId = analyzedByUser.getId();
        }
    }

    public BigDecimal getCurrentRatio() {
        return currentRatio;
    }

    public void setCurrentRatio(BigDecimal currentRatio) {
        this.currentRatio = currentRatio;
    }

    public BigDecimal getQuickRatio() {
        return quickRatio;
    }

    public void setQuickRatio(BigDecimal quickRatio) {
        this.quickRatio = quickRatio;
    }

    public BigDecimal getCashRatio() {
        return cashRatio;
    }

    public void setCashRatio(BigDecimal cashRatio) {
        this.cashRatio = cashRatio;
    }

    public BigDecimal getDebtToEquityRatio() {
        return debtToEquityRatio;
    }

    public void setDebtToEquityRatio(BigDecimal debtToEquityRatio) {
        this.debtToEquityRatio = debtToEquityRatio;
    }

    public BigDecimal getTotalDebtToTotalAssetsRatio() {
        return totalDebtToTotalAssetsRatio;
    }

    public void setTotalDebtToTotalAssetsRatio(BigDecimal totalDebtToTotalAssetsRatio) {
        this.totalDebtToTotalAssetsRatio = totalDebtToTotalAssetsRatio;
    }

    public String getReportData() {
        return reportData;
    }

    public void setReportData(String reportData) {
        this.reportData = reportData;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AnalysisReport that = (AnalysisReport) o;
        return id == that.id && bankId == that.bankId && analyzedByUserId == that.analyzedByUserId && Objects.equals(reportType, that.reportType) && Objects.equals(analysisDate, that.analysisDate) && Objects.equals(currentRatio, that.currentRatio) && Objects.equals(quickRatio, that.quickRatio) && Objects.equals(cashRatio, that.cashRatio) && Objects.equals(debtToEquityRatio, that.debtToEquityRatio) && Objects.equals(totalDebtToTotalAssetsRatio, that.totalDebtToTotalAssetsRatio) && Objects.equals(reportData, that.reportData);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, bankId, reportType, analysisDate, analyzedByUserId, currentRatio, quickRatio, cashRatio, debtToEquityRatio, totalDebtToTotalAssetsRatio, reportData);
    }

    @Override
    public String toString() {
        return "AnalysisReport{" +
                "id=" + id +
                ", bankId=" + bankId +
                ", reportType='" + reportType + '\'' +
                ", analysisDate=" + analysisDate +
                ", analyzedByUserId=" + analyzedByUserId +
                '}';
    }
}