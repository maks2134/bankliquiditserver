package com.bsuir.bankliquiditserver.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

public class LiquidityAnalysisResultDTO implements Serializable {
    private static final long serialVersionUID = 201L;

    private int bankId;
    private String bankName;
    private LocalDate reportDate; // Дата фин. отчета, на основе которого сделан анализ

    private BigDecimal currentRatio;
    private BigDecimal quickRatio;
    private BigDecimal cashRatio;
    // Можно добавить текстовые интерпретации или пороговые значения
    private String currentRatioInterpretation;
    private String quickRatioInterpretation;
    private String cashRatioInterpretation;

    // Геттеры и сеттеры
    public int getBankId() { return bankId; }
    public void setBankId(int bankId) { this.bankId = bankId; }
    public String getBankName() { return bankName; }
    public void setBankName(String bankName) { this.bankName = bankName; }
    public LocalDate getReportDate() { return reportDate; }
    public void setReportDate(LocalDate reportDate) { this.reportDate = reportDate; }
    public BigDecimal getCurrentRatio() { return currentRatio; }
    public void setCurrentRatio(BigDecimal currentRatio) { this.currentRatio = currentRatio; }
    public BigDecimal getQuickRatio() { return quickRatio; }
    public void setQuickRatio(BigDecimal quickRatio) { this.quickRatio = quickRatio; }
    public BigDecimal getCashRatio() { return cashRatio; }
    public void setCashRatio(BigDecimal cashRatio) { this.cashRatio = cashRatio; }
    public String getCurrentRatioInterpretation() { return currentRatioInterpretation; }
    public void setCurrentRatioInterpretation(String currentRatioInterpretation) { this.currentRatioInterpretation = currentRatioInterpretation; }
    public String getQuickRatioInterpretation() { return quickRatioInterpretation; }
    public void setQuickRatioInterpretation(String quickRatioInterpretation) { this.quickRatioInterpretation = quickRatioInterpretation; }
    public String getCashRatioInterpretation() { return cashRatioInterpretation; }
    public void setCashRatioInterpretation(String cashRatioInterpretation) { this.cashRatioInterpretation = cashRatioInterpretation; }
}