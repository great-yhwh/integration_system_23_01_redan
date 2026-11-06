package ru.student35.sync.repository;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import ru.student35.sync.model.Part;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@DisplayName("Интеграционное тестирование взаимодействия с бд")
class PartRepositoryTest {

    @Autowired
    private PartRepository partRepository;

    @AfterEach
    @DisplayName("Очистка базы данных после каждого теста")
    void cleanUp() {
        partRepository.deleteAll();
    }

    private Part createPart(String code, String name, String price, boolean isDeleted) {
        Part part = new Part();
        part.setSpareCode(code);
        part.setSpareName(name);
        part.setSpareDescription("Description for " + name);
        part.setSpareType("BRAKE");
        part.setSpareStatus("AVAILABLE");
        part.setPrice(new BigDecimal(price));
        part.setQuantity(100);
        part.setUpdatedAt("2026-09-28T12:35:25.238411853");
        part.setDeleted(isDeleted);
        return part;
    }

    /**
     * Тест: успешное сохранение детали и поиск её по коду из CMS
     */
    @Test
    @DisplayName("Сохранение детали и её поиск по коду")
    void testSaveAndFindActivePart() {
        Part targetPart = createPart("SP12345", "Brake Pad", "15.99", false);

        partRepository.save(targetPart);
        Optional<Part> found = partRepository.findBySpareCode("SP12345");

        assertThat(found)
                .isPresent()
                .hasValueSatisfying(part -> {
                    assertThat(part.getSpareName()).isEqualTo("Brake Pad");
                    assertThat(part.getPrice()).isEqualByComparingTo("15.99");
                    assertThat(part.isDeleted()).isFalse();
                });
    }
    /**
     * Тест: удаленная деталь скрывается из активных, но остается в БД
     * (Пометка на удаление)
     */
    @Test
    @DisplayName("Удаленная деталь остается в БД, но не попадает в выборку активных")
    void testSoftDeleteMechanism() {
        Part activePart = createPart("SP11111", "Active Spark Plug", "5.50", false);
        Part deletedPart = createPart("SP22222", "Deleted Filter", "12.00", true);

        partRepository.save(activePart);
        partRepository.save(deletedPart);

        List<Part> activeParts = partRepository.findByIsDeletedFalse();
        Optional<Part> foundDeleted = partRepository.findBySpareCode("SP22222");

        assertThat(activeParts)
                .hasSize(1)
                .extracting(Part::getSpareCode)
                .containsExactly("SP11111");

        assertThat(foundDeleted).isPresent();
        assertThat(foundDeleted.get().isDeleted()).isTrue();
    }

    @Test
    @DisplayName("Повторное сохранение обновляет деталь без создания дубликата")
    void testUpdateExistingPartOnResync() {
        Part originalPart = createPart("SP12345", "Brake Pad", "15.99", false);
        partRepository.save(originalPart);

        Part existing = partRepository.findBySpareCode("SP12345").orElseThrow();
        existing.setPrice(new BigDecimal("19.99"));
        partRepository.save(existing);

        assertThat(partRepository.findAll()).hasSize(1);

        Part updated = partRepository.findBySpareCode("SP12345").orElseThrow();
        assertThat(updated.getPrice()).isEqualByComparingTo("19.99");
        assertThat(updated.getSpareName()).isEqualTo("Brake Pad");
    }
}