package ru.student35.sync.dto.cms;

/**
 * DTO для десериализации данных, получаемых из внешней системы CMS.
 *
 * <p>Пример входящего JSON из SWAGGER:</p>
 * <pre>{@code
 * [
 *   {
 *     "spareCode": "SP12345",
 *     "spareName": "Brake Pad",
 *     "spareDescription": "Передние тормозные колодки",
 *     "spareType": "RADIATOR",
 *     "spareStatus": "AVAILABLE",
 *     "price": "15.99",
 *     "quantity": 100,
 *     "updatedAt": "2025-09-09"
 *   }
 * ]
 * }</pre>
 */
public class SpareDto {
    private String spareCode;
    private String spareName;
    private String spareDescription;
    private String spareType;
    private String spareStatus;
    private String price;
    private Integer quantity;
    private String updatedAt;

    public SpareDto() {
    }

    public String getSpareCode() {
        return spareCode;
    }

    public void setSpareCode(String spareCode) {
        this.spareCode = spareCode;
    }

    public String getSpareName() {
        return spareName;
    }

    public void setSpareName(String spareName) {
        this.spareName = spareName;
    }

    public String getSpareDescription() {
        return spareDescription;
    }

    public void setSpareDescription(String spareDescription) {
        this.spareDescription = spareDescription;
    }

    public String getSpareType() {
        return spareType;
    }

    public void setSpareType(String spareType) {
        this.spareType = spareType;
    }

    public String getSpareStatus() {
        return spareStatus;
    }

    public void setSpareStatus(String spareStatus) {
        this.spareStatus = spareStatus;
    }

    public String getPrice() {
        return price;
    }

    public void setPrice(String price) {
        this.price = price;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }
}