package com.bsuir.bankliquiditserver.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

public class LiquidityAnalysisResultDTO implements Serializable {
    private static final long serialVersionUID = 201L;

    private int bankId;
    private String bankName;
    private LocalDate reportDate; // Дата фин. отчета, на основе которого сделан анализ

    private BigDecimal currentRatio;
    private BigDecimal quickRatio;
    private BigDecimal cashRatio;

    private String currentRatioInterpretation;
    private String quickRatioInterpretation;
    private String cashRatioInterpretation;

    public LiquidityAnalysisResultDTO() {}

    // Геттеры
    public int getBankId() { return bankId; }
    public String getBankName() { return bankName; }
    public LocalDate getReportDate() { return reportDate; }
    public BigDecimal getCurrentRatio() { return currentRatio; }
    public BigDecimal getQuickRatio() { return quickRatio; }
    public BigDecimal getCashRatio() { return cashRatio; }
    public String getCurrentRatioInterpretation() { return currentRatioInterpretation; }
    public String getQuickRatioInterpretation() { return quickRatioInterpretation; }
    public String getCashRatioInterpretation() { return cashRatioInterpretation; }

    // Сеттеры
    public void setBankId(int bankId) { this.bankId = bankId; }
    public void setBankName(String bankName) { this.bankName = bankName; }
    public void setReportDate(LocalDate reportDate) { this.reportDate = reportDate; }
    public void setCurrentRatio(BigDecimal currentRatio) { this.currentRatio = currentRatio; }
    public void setQuickRatio(BigDecimal quickRatio) { this.quickRatio = quickRatio; }
    public void setCashRatio(BigDecimal cashRatio) { this.cashRatio = cashRatio; }
    public void setCurrentRatioInterpretation(String currentRatioInterpretation) { this.currentRatioInterpretation = currentRatioInterpretation; }
    public void setQuickRatioInterpretation(String quickRatioInterpretation) { this.quickRatioInterpretation = quickRatioInterpretation; }
    public void setCashRatioInterpretation(String cashRatioInterpretation) { this.cashRatioInterpretation = cashRatioInterpretation; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LiquidityAnalysisResultDTO that = (LiquidityAnalysisResultDTO) o;
        return bankId == that.bankId &&
                Objects.equals(reportDate, that.reportDate) &&
                // Сравнение BigDecimal через compareTo
                (currentRatio == null ? that.currentRatio == null : currentRatio.compareTo(that.currentRatio) == 0) &&
                (quickRatio == null ? that.quickRatio == null : quickRatio.compareTo(that.quickRatio) == 0) &&
                (cashRatio == null ? that.cashRatio == null : cashRatio.compareTo(that.cashRatio) == 0);
    }

    @Override
    public int hashCode() {
        return Objects.hash(bankId, reportDate,
                (currentRatio != null ? currentRatio.stripTrailingZeros() : null),
                (quickRatio != null ? quickRatio.stripTrailingZeros() : null),
                (cashRatio != null ? cashRatio.stripTrailingZeros() : null));
    }

    @Override
    public String toString() {
        return "LiquidityAnalysisResultDTO{" +
                "bankId=" + bankId +
                ", bankName='" + bankName + '\'' +
                ", reportDate=" + reportDate +
                ", currentRatio=" + currentRatio +
                ", quickRatio=" + quickRatio +
                ", cashRatio=" + cashRatio +
                '}';
    }
}