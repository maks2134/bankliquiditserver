package com.bsuir.bankliquiditserver.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Objects;

public class StatementItemDTO implements Serializable {
    private static final long serialVersionUID = 101L;

    private Integer id; // Может быть null для новых статей
    private String itemCode;
    private String itemName;
    private BigDecimal itemValue;
    private Integer parentItemId; // Для иерархии

    public StatementItemDTO() {
    }

    // Конструктор для удобства
    public StatementItemDTO(String itemCode, String itemName, BigDecimal itemValue) {
        this.itemCode = itemCode;
        this.itemName = itemName;
        this.itemValue = itemValue;
    }

    // Геттеры и Сеттеры
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getItemCode() { return itemCode; }
    public void setItemCode(String itemCode) { this.itemCode = itemCode; }
    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }
    public BigDecimal getItemValue() { return itemValue; }
    public void setItemValue(BigDecimal itemValue) { this.itemValue = itemValue; }
    public Integer getParentItemId() { return parentItemId; }
    public void setParentItemId(Integer parentItemId) { this.parentItemId = parentItemId; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StatementItemDTO that = (StatementItemDTO) o;
        return Objects.equals(id, that.id) && Objects.equals(itemCode, that.itemCode) && Objects.equals(itemName, that.itemName) && Objects.equals(itemValue, that.itemValue) && Objects.equals(parentItemId, that.parentItemId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, itemCode, itemName, itemValue, parentItemId);
    }

    @Override
    public String toString() {
        return "StatementItemDTO{" +
                "id=" + id +
                ", itemCode='" + itemCode + '\'' +
                ", itemName='" + itemName + '\'' +
                ", itemValue=" + itemValue +
                ", parentItemId=" + parentItemId +
                '}';
    }
}