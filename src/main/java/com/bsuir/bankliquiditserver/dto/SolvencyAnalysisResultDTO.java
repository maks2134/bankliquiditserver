package com.bsuir.bankliquiditserver.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

public class SolvencyAnalysisResultDTO implements Serializable {
    private static final long serialVersionUID = 202L;

    private int bankId;
    private String bankName;
    private LocalDate reportDate;

    private BigDecimal equityRatio; // Коэффициент автономии (СК / Активы)
    private BigDecimal debtToEquityRatio; // Заемный капитал / Собственный капитал
    // Добавим еще один из SQL схемы для примера total_debt_to_total_assets_ratio
    private BigDecimal totalDebtToTotalAssetsRatio; // Общие обязательства / Активы

    private String equityRatioInterpretation;
    private String debtToEquityRatioInterpretation;
    private String totalDebtToTotalAssetsRatioInterpretation;


    public SolvencyAnalysisResultDTO() {}

    // Геттеры
    public int getBankId() { return bankId; }
    public String getBankName() { return bankName; }
    public LocalDate getReportDate() { return reportDate; }
    public BigDecimal getEquityRatio() { return equityRatio; }
    public BigDecimal getDebtToEquityRatio() { return debtToEquityRatio; }
    public BigDecimal getTotalDebtToTotalAssetsRatio() { return totalDebtToTotalAssetsRatio; }
    public String getEquityRatioInterpretation() { return equityRatioInterpretation; }
    public String getDebtToEquityRatioInterpretation() { return debtToEquityRatioInterpretation; }
    public String getTotalDebtToTotalAssetsRatioInterpretation() { return totalDebtToTotalAssetsRatioInterpretation; }


    // Сеттеры
    public void setBankId(int bankId) { this.bankId = bankId; }
    public void setBankName(String bankName) { this.bankName = bankName; }
    public void setReportDate(LocalDate reportDate) { this.reportDate = reportDate; }
    public void setEquityRatio(BigDecimal equityRatio) { this.equityRatio = equityRatio; }
    public void setDebtToEquityRatio(BigDecimal debtToEquityRatio) { this.debtToEquityRatio = debtToEquityRatio; }
    public void setTotalDebtToTotalAssetsRatio(BigDecimal totalDebtToTotalAssetsRatio) { this.totalDebtToTotalAssetsRatio = totalDebtToTotalAssetsRatio; }
    public void setEquityRatioInterpretation(String equityRatioInterpretation) { this.equityRatioInterpretation = equityRatioInterpretation; }
    public void setDebtToEquityRatioInterpretation(String debtToEquityRatioInterpretation) { this.debtToEquityRatioInterpretation = debtToEquityRatioInterpretation; }
    public void setTotalDebtToTotalAssetsRatioInterpretation(String totalDebtToTotalAssetsRatioInterpretation) { this.totalDebtToTotalAssetsRatioInterpretation = totalDebtToTotalAssetsRatioInterpretation; }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SolvencyAnalysisResultDTO that = (SolvencyAnalysisResultDTO) o;
        return bankId == that.bankId &&
                Objects.equals(reportDate, that.reportDate) &&
                (equityRatio == null ? that.equityRatio == null : equityRatio.compareTo(that.equityRatio) == 0) &&
                (debtToEquityRatio == null ? that.debtToEquityRatio == null : debtToEquityRatio.compareTo(that.debtToEquityRatio) == 0) &&
                (totalDebtToTotalAssetsRatio == null ? that.totalDebtToTotalAssetsRatio == null : totalDebtToTotalAssetsRatio.compareTo(that.totalDebtToTotalAssetsRatio) == 0);
    }

    @Override
    public int hashCode() {
        return Objects.hash(bankId, reportDate,
                (equityRatio != null ? equityRatio.stripTrailingZeros() : null),
                (debtToEquityRatio != null ? debtToEquityRatio.stripTrailingZeros() : null),
                (totalDebtToTotalAssetsRatio != null ? totalDebtToTotalAssetsRatio.stripTrailingZeros() : null));
    }

    @Override
    public String toString() {
        return "SolvencyAnalysisResultDTO{" +
                "bankId=" + bankId +
                ", bankName='" + bankName + '\'' +
                ", reportDate=" + reportDate +
                ", equityRatio=" + equityRatio +
                ", debtToEquityRatio=" + debtToEquityRatio +
                ", totalDebtToTotalAssetsRatio=" + totalDebtToTotalAssetsRatio +
                '}';
    }
}