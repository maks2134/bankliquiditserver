package com.bsuir.bankliquiditserver.dto;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class FinancialStatementDTO implements Serializable {
    private static final long serialVersionUID = 102L;

    private Integer id; // Может быть null для новых
    private int bankId;
    private String bankName; // Для отображения на клиенте
    private LocalDate reportDate;
    private String statementType; // 'BALANCE_SHEET', 'INCOME_STATEMENT'
    private String currency;
    private LocalDateTime createdAt;
    private Integer createdByUserId;
    private String createdByUsername; // Для отображения

    private List<StatementItemDTO> items = new ArrayList<>();

    public FinancialStatementDTO() {
    }

    // Геттеры и Сеттеры
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public int getBankId() { return bankId; }
    public void setBankId(int bankId) { this.bankId = bankId; }
    public String getBankName() { return bankName; }
    public void setBankName(String bankName) { this.bankName = bankName; }
    public LocalDate getReportDate() { return reportDate; }
    public void setReportDate(LocalDate reportDate) { this.reportDate = reportDate; }
    public String getStatementType() { return statementType; }
    public void setStatementType(String statementType) { this.statementType = statementType; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public Integer getCreatedByUserId() { return createdByUserId; }
    public void setCreatedByUserId(Integer createdByUserId) { this.createdByUserId = createdByUserId; }
    public String getCreatedByUsername() { return createdByUsername; }
    public void setCreatedByUsername(String createdByUsername) { this.createdByUsername = createdByUsername; }
    public List<StatementItemDTO> getItems() { return items; }
    public void setItems(List<StatementItemDTO> items) { this.items = items; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FinancialStatementDTO that = (FinancialStatementDTO) o;
        return bankId == that.bankId && Objects.equals(id, that.id) && Objects.equals(reportDate, that.reportDate) && Objects.equals(statementType, that.statementType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, bankId, reportDate, statementType);
    }

    @Override
    public String toString() {
        return "FinancialStatementDTO{" +
                "id=" + id +
                ", bankId=" + bankId +
                ", reportDate=" + reportDate +
                ", statementType='" + statementType + '\'' +
                ", itemsCount=" + (items != null ? items.size() : 0) +
                '}';
    }
}