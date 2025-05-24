package com.bsuir.bankliquiditserver.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

public class SolvencyAnalysisResultDTO implements Serializable {
    private static final long serialVersionUID = 202L;

    private int bankId;
    private String bankName;
    private LocalDate reportDate;

    private BigDecimal equityRatio;
    private BigDecimal debtToEquityRatio;
    // private BigDecimal interestCoverageRatio; // Если будет Отчет о прибылях и убытках
    private String equityRatioInterpretation;
    private String debtToEquityRatioInterpretation;

    // Геттеры и сеттеры
    public int getBankId() { return bankId; }
    public void setBankId(int bankId) { this.bankId = bankId; }
    public String getBankName() { return bankName; }
    public void setBankName(String bankName) { this.bankName = bankName; }
    public LocalDate getReportDate() { return reportDate; }
    public void setReportDate(LocalDate reportDate) { this.reportDate = reportDate; }
    public BigDecimal getEquityRatio() { return equityRatio; }
    public void setEquityRatio(BigDecimal equityRatio) { this.equityRatio = equityRatio; }
    public BigDecimal getDebtToEquityRatio() { return debtToEquityRatio; }
    public void setDebtToEquityRatio(BigDecimal debtToEquityRatio) { this.debtToEquityRatio = debtToEquityRatio; }
    public String getEquityRatioInterpretation() { return equityRatioInterpretation; }
    public void setEquityRatioInterpretation(String equityRatioInterpretation) { this.equityRatioInterpretation = equityRatioInterpretation; }
    public String getDebtToEquityRatioInterpretation() { return debtToEquityRatioInterpretation; }
    public void setDebtToEquityRatioInterpretation(String debtToEquityRatioInterpretation) { this.debtToEquityRatioInterpretation = debtToEquityRatioInterpretation; }
}