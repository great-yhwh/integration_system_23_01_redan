package ru.student35.sync.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Доменная сущность, представляющая историческую запись о детали в БД.
 *
 * <p>Класс реализует концепцию сохранения историчности данных:</p>
 * <ul>
 *   <li>Если деталь обновляется в CMS — обновляются соответствующие поля и дата {@code syncedAt}.</li>
 *   <li>Если деталь удаляется из CMS — физического удаления из БД не происходит. Сущность помечается флагом
 *       {@code isDeleted = true}, и фиксируется дата удаления {@code deletedAt}.</li>
 * </ul>
 *
 * <p>Хранение данных в типизированном виде (например, {@link BigDecimal} для цен и {@link LocalDate} для дат)
 * для возможности матрасчетов и фильтраций на уровне БД.</p>
 */
@Document(collection = "parts")
public class Part {

    @Id
    private String id;

    @Indexed(unique = true)
    private String spareCode;

    private String spareName;
    private String spareDescription;
    private String spareType;
    private String spareStatus;
    private BigDecimal price;
    private Integer quantity;
    private LocalDate updatedAt;

    private boolean isDeleted;
    private LocalDateTime syncedAt;
    private LocalDateTime deletedAt;

    public Part() {
    }

    // Геттеры и Сеттеры
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public LocalDate getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDate updatedAt) {
        this.updatedAt = updatedAt;
    }

    public boolean isDeleted() {
        return isDeleted;
    }

    public void setDeleted(boolean deleted) {
        isDeleted = deleted;
    }

    public LocalDateTime getSyncedAt() {
        return syncedAt;
    }

    public void setSyncedAt(LocalDateTime syncedAt) {
        this.syncedAt = syncedAt;
    }

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }
}