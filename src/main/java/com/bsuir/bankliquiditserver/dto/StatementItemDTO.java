package com.bsuir.bankliquiditserver.dto;

import com.bsuir.bankliquiditserver.model.StatementItem;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Objects;

public class StatementItemDTO implements Serializable {
    private static final long serialVersionUID = 101L;

    private Integer id; // Может быть null для новых статей при создании отчета
    private String itemCode;
    private String itemName;
    private BigDecimal itemValue;
    private Integer parentItemId; // Для иерархии

    public StatementItemDTO() {
    }

    // Конструктор для удобства создания на клиенте или в тестах
    public StatementItemDTO(String itemCode, String itemName, BigDecimal itemValue, Integer parentItemId) {
        this.itemCode = itemCode;
        this.itemName = itemName;
        this.itemValue = itemValue;
        this.parentItemId = parentItemId;
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

    // Метод для преобразования из сущности StatementItem в StatementItemDTO
    public static StatementItemDTO fromStatementItem(StatementItem item) {
        if (item == null) {
            return null;
        }
        StatementItemDTO dto = new StatementItemDTO();
        dto.setId(item.getId());
        dto.setItemCode(item.getItemCode());
        dto.setItemName(item.getItemName());
        dto.setItemValue(item.getItemValue());
        dto.setParentItemId(item.getParentItemId());
        return dto;
    }

    // Метод для преобразования из StatementItemDTO в сущность StatementItem
    // (может понадобиться на сервере при получении DTO от клиента для создания/обновления)
    public StatementItem toStatementItem() {
        StatementItem item = new StatementItem();
        item.setId(this.id); // ID будет null для новых, что нормально для DAO.save()
        item.setItemCode(this.itemCode);
        item.setItemName(this.itemName);
        item.setItemValue(this.itemValue);
        item.setParentItemId(this.parentItemId);
        // statementId устанавливается отдельно, когда статья привязывается к отчету
        return item;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StatementItemDTO that = (StatementItemDTO) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(itemCode, that.itemCode) &&
                Objects.equals(itemName, that.itemName) &&
                // Сравнение BigDecimal через compareTo для точности
                (itemValue == null ? that.itemValue == null : itemValue.compareTo(that.itemValue) == 0) &&
                Objects.equals(parentItemId, that.parentItemId);
    }

    @Override
    public int hashCode() {
        // Для BigDecimal используем его собственный hashCode, предварительно обработав null
        return Objects.hash(id, itemCode, itemName, (itemValue != null ? itemValue.stripTrailingZeros() : null), parentItemId);
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