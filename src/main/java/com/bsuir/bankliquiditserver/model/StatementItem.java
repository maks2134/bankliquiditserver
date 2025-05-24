package com.bsuir.bankliquiditserver.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Objects;

public class StatementItem implements Serializable {
    private static final long serialVersionUID = 5L;

    private int id;
    private int statementId;
    private String itemCode;
    private String itemName;
    private BigDecimal itemValue;
    private Integer parentItemId; // Может быть null

    // Конструкторы, геттеры/сеттеры, equals/hashCode/toString
    public StatementItem() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getStatementId() {
        return statementId;
    }

    public void setStatementId(int statementId) {
        this.statementId = statementId;
    }

    public String getItemCode() {
        return itemCode;
    }

    public void setItemCode(String itemCode) {
        this.itemCode = itemCode;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public BigDecimal getItemValue() {
        return itemValue;
    }

    public void setItemValue(BigDecimal itemValue) {
        this.itemValue = itemValue;
    }

    public Integer getParentItemId() {
        return parentItemId;
    }

    public void setParentItemId(Integer parentItemId) {
        this.parentItemId = parentItemId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StatementItem that = (StatementItem) o;
        return id == that.id && statementId == that.statementId && Objects.equals(itemCode, that.itemCode) && Objects.equals(itemName, that.itemName) && Objects.equals(itemValue, that.itemValue) && Objects.equals(parentItemId, that.parentItemId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, statementId, itemCode, itemName, itemValue, parentItemId);
    }

    @Override
    public String toString() {
        return "StatementItem{" +
                "id=" + id +
                ", statementId=" + statementId +
                ", itemCode='" + itemCode + '\'' +
                ", itemName='" + itemName + '\'' +
                ", itemValue=" + itemValue +
                ", parentItemId=" + parentItemId +
                '}';
    }
}