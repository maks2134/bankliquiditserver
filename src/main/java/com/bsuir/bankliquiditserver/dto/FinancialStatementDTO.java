package com.bsuir.bankliquiditserver.dto;

import com.bsuir.bankliquiditserver.model.FinancialStatement;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

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

    // Конструктор для удобства (может быть полезен)
    public FinancialStatementDTO(Integer id, int bankId, String bankName, LocalDate reportDate, String statementType, String currency, LocalDateTime createdAt, Integer createdByUserId, String createdByUsername, List<StatementItemDTO> items) {
        this.id = id;
        this.bankId = bankId;
        this.bankName = bankName;
        this.reportDate = reportDate;
        this.statementType = statementType;
        this.currency = currency;
        this.createdAt = createdAt;
        this.createdByUserId = createdByUserId;
        this.createdByUsername = createdByUsername;
        if (items != null) {
            this.items = items;
        }
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

    // Метод для преобразования из сущности FinancialStatement в FinancialStatementDTO
    public static FinancialStatementDTO fromFinancialStatement(FinancialStatement statement, boolean includeItems) {
        if (statement == null) {
            return null;
        }
        FinancialStatementDTO dto = new FinancialStatementDTO();
        dto.setId(statement.getId());
        dto.setBankId(statement.getBankId());
        if (statement.getBank() != null) {
            dto.setBankName(statement.getBank().getName());
        }
        dto.setReportDate(statement.getReportDate());
        dto.setStatementType(statement.getStatementType());
        dto.setCurrency(statement.getCurrency());
        dto.setCreatedAt(statement.getCreatedAt());
        dto.setCreatedByUserId(statement.getCreatedByUserId());
        if (statement.getCreatedByUser() != null) {
            dto.setCreatedByUsername(statement.getCreatedByUser().getUsername());
        }

        if (includeItems && statement.getItems() != null && !statement.getItems().isEmpty()) {
            dto.setItems(
                    statement.getItems().stream()
                            .map(StatementItemDTO::fromStatementItem)
                            .collect(Collectors.toList())
            );
        } else {
            dto.setItems(new ArrayList<>()); // Инициализируем пустым списком, чтобы избежать NullPointerException
        }
        return dto;
    }

    // Метод для преобразования из FinancialStatementDTO в сущность FinancialStatement
    // (может понадобиться на сервере при получении DTO от клиента для создания/обновления)
    public FinancialStatement toFinancialStatement() {
        FinancialStatement statement = new FinancialStatement();
        statement.setId(this.id); // ID будет null для новых
        statement.setBankId(this.bankId);
        // Объекты Bank и User (createdByUser) должны быть установлены в сервисе при необходимости
        statement.setReportDate(this.reportDate);
        statement.setStatementType(this.statementType);
        statement.setCurrency(this.currency);
        statement.setCreatedAt(this.createdAt); // Обычно createdAt устанавливается сервером при создании
        statement.setCreatedByUserId(this.createdByUserId);

        if (this.items != null) {
            statement.setItems(
                    this.items.stream()
                            .map(StatementItemDTO::toStatementItem)
                            .collect(Collectors.toList())
            );
        }
        return statement;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FinancialStatementDTO that = (FinancialStatementDTO) o;
        // Сравниваем основные идентификационные поля. Список items обычно не сравнивается в equals.
        return bankId == that.bankId &&
                Objects.equals(id, that.id) &&
                Objects.equals(reportDate, that.reportDate) &&
                Objects.equals(statementType, that.statementType) &&
                Objects.equals(currency, that.currency);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, bankId, reportDate, statementType, currency);
    }

    @Override
    public String toString() {
        return "FinancialStatementDTO{" +
                "id=" + id +
                ", bankId=" + bankId +
                (bankName != null ? ", bankName='" + bankName + '\'' : "") +
                ", reportDate=" + reportDate +
                ", statementType='" + statementType + '\'' +
                ", currency='" + currency + '\'' +
                ", createdAt=" + createdAt +
                (createdByUsername != null ? ", createdByUsername='" + createdByUsername + '\'' : (createdByUserId != null ? ", createdByUserId=" + createdByUserId : "")) +
                ", itemsCount=" + (items != null ? items.size() : 0) +
                '}';
    }
}