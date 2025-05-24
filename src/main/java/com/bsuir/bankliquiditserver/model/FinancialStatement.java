package com.bsuir.bankliquiditserver.model;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class FinancialStatement implements Serializable {
    private static final long serialVersionUID = 4L;

    private int id;
    private int bankId;
    private Bank bank; // Объект банка
    private LocalDate reportDate;
    private String statementType; // 'BALANCE_SHEET', 'INCOME_STATEMENT'
    private String currency;
    private LocalDateTime createdAt;
    private Integer createdByUserId; // Может быть null
    private User createdByUser; // Объект пользователя

    private List<StatementItem> items = new ArrayList<>(); // Список статей отчета

    // Конструкторы, геттеры/сеттеры, equals/hashCode/toString
    public FinancialStatement() {
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

    public LocalDate getReportDate() {
        return reportDate;
    }

    public void setReportDate(LocalDate reportDate) {
        this.reportDate = reportDate;
    }

    public String getStatementType() {
        return statementType;
    }

    public void setStatementType(String statementType) {
        this.statementType = statementType;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Integer getCreatedByUserId() {
        return createdByUserId;
    }

    public void setCreatedByUserId(Integer createdByUserId) {
        this.createdByUserId = createdByUserId;
    }

    public User getCreatedByUser() {
        return createdByUser;
    }

    public void setCreatedByUser(User createdByUser) {
        this.createdByUser = createdByUser;
        if (createdByUser != null) {
            this.createdByUserId = createdByUser.getId();
        }
    }

    public List<StatementItem> getItems() {
        return items;
    }

    public void setItems(List<StatementItem> items) {
        this.items = items;
    }

    public void addItem(StatementItem item) {
        this.items.add(item);
        item.setStatementId(this.id); // Связываем статью с отчетом
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FinancialStatement that = (FinancialStatement) o;
        return id == that.id && bankId == that.bankId && Objects.equals(reportDate, that.reportDate) && Objects.equals(statementType, that.statementType) && Objects.equals(currency, that.currency) && Objects.equals(createdAt, that.createdAt) && Objects.equals(createdByUserId, that.createdByUserId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, bankId, reportDate, statementType, currency, createdAt, createdByUserId);
    }

    @Override
    public String toString() {
        return "FinancialStatement{" +
                "id=" + id +
                ", bankId=" + bankId +
                ", reportDate=" + reportDate +
                ", statementType='" + statementType + '\'' +
                ", currency='" + currency + '\'' +
                ", createdAt=" + createdAt +
                ", createdByUserId=" + createdByUserId +
                ", itemsCount=" + (items != null ? items.size() : 0) +
                '}';
    }
}