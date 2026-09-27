package ru.student35.sync.repository;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import ru.student35.sync.model.Part;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class PartRepositoryTest {

    @Autowired
    private PartRepository partRepository;

    @AfterEach
    void cleanUp() {
        partRepository.deleteAll();
    }

    private Part createActivePart(String code, String name, String price) {
        Part part = new Part();
        part.setSpareCode(code);
        part.setSpareName(name);
        part.setSpareDescription("Description for " + name);
        part.setSpareType("BRAKE");
        part.setSpareStatus("AVAILABLE");
        part.setPrice(new BigDecimal(price));
        part.setQuantity(100);
        part.setUpdatedAt(LocalDate.now());
        part.setDeleted(false);
        return part;
    }

    /**
     * Тест: успешное сохранение детали и поиск её по коду из CMS
     */
    @Test
    void testSaveAndFindActivePart() {
        partRepository.save(createActivePart("SP12345", "Brake Pad", "15.99"));

        Optional<Part> found = partRepository.findBySpareCode("SP12345");

        assertThat(found).isPresent();
        assertThat(found.get().getSpareName()).isEqualTo("Brake Pad");
        assertThat(found.get().isDeleted()).isFalse();
    }
    /**
     * Тест: удаленная деталь скрывается из активных, но остается в БД
     * (Пометка на удаление)
     */
    @Test
    void testSoftDeleteMechanism() {
        Part part = createActivePart("SP99999", "Old Part", "10.00");
        partRepository.save(part);

        part.setDeleted(true);
        partRepository.save(part);

        assertThat(partRepository.findByIsDeletedFalse()).isEmpty();
        assertThat(partRepository.findBySpareCode("SP99999")).isPresent();
    }
    /**
     * Тест: при обновлении полей дубликат в БД не создается
     */
    @Test
    void testUpdateExistingPartOnResync() {
        /*  */
        partRepository.save(createActivePart("SP12345", "Brake Pad", "15.99"));

        Part existing = partRepository.findBySpareCode("SP12345").orElseThrow();
        existing.setPrice(new BigDecimal("19.99"));
        partRepository.save(existing);

        assertThat(partRepository.findAll()).hasSize(1);
        Part updated = partRepository.findBySpareCode("SP12345").orElseThrow();
        assertThat(updated.getPrice()).isEqualByComparingTo("19.99");
    }
}