package ru.student35.sync.routes;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.AdviceWith;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.apache.camel.test.spring.junit5.UseAdviceWith;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.ActiveProfiles;
import ru.student35.sync.model.Part;
import ru.student35.sync.repository.PartRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Интеграционный тест Content-Based Router в роуте.
 *
 * <p>Проверяет все 4 ветки маршрутизации:</p>
 * <ul>
 *   <li>INSERT — новая деталь</li>
 *   <li>SKIP — неизменная деталь</li>
 *   <li>UPDATE — изменённая дата</li>
 *   <li>SOFT DELETE — деталь отсутствует в CMS</li>
 * </ul>
 */
@CamelSpringBootTest
@SpringBootTest
@UseAdviceWith
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Интеграционное тестирование роута: CMS → MongoDB")
class CmsSyncRouteTest {
    private static final Logger log = LoggerFactory.getLogger(CmsSyncRouteTest.class);

    private static final int TOTAL_CMS_RECORDS = 50;

    private static Process mockProcess;

    @Autowired
    private CamelContext camelContext;

    @Autowired
    private ProducerTemplate producerTemplate;

    @Autowired
    private PartRepository partRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

    @BeforeAll
    @DisplayName("Запуск мок-сервера CMS")
    static void startMock() throws Exception {
        ProcessBuilder pb = new ProcessBuilder("node", "src/test/resources/mocks/cms-mock.js");
        pb.redirectErrorStream(true);
        mockProcess = pb.start();
        Thread.sleep(1000);

        assertThat(mockProcess.isAlive())
                .as("Мок-сервер CMS должен успешно запуститься")
                .isTrue();
    }

    @AfterAll
    @DisplayName("Остановка мок-сервера CMS")
    static void stopMock() {
        if (mockProcess != null) {
            mockProcess.destroyForcibly();
        }
    }

    private static boolean adviceApplied = false;

    @BeforeEach
    @DisplayName("Очистка БД и подготовка роута")
    void setUp() throws Exception {
        mongoTemplate.dropCollection(Part.class);

        if (!adviceApplied) {
            AdviceWith.adviceWith(camelContext, "cmsSync", a ->
                    a.replaceFromWith("direct:triggerSync")
            );
            adviceApplied = true;
        }

        if (!camelContext.isStarted()) {
            camelContext.start();
        }
    }

    // ─────────────────────────── INSERT ───────────────────────────

    @Test
    @Order(1)
    @DisplayName("CBR → INSERT: первый запуск, все записи новые")
    void firstRun_shouldInsertAll() {
        producerTemplate.sendBody("direct:triggerSync", null);

        long count = partRepository.count();

        assertThat(count)
                .as("Мок отдаёт %d деталей (5 страниц × 10)", TOTAL_CMS_RECORDS)
                .isEqualTo(TOTAL_CMS_RECORDS);

        Part first = partRepository.findBySpareCode("SPARE-1").orElseThrow();

        assertThat(first.isDeleted())
                .as("Новая деталь не должна быть помечена как удалённая")
                .isFalse();

        assertThat(first.getSyncedAt())
                .as("syncedAt должен быть заполнен")
                .isNotNull();

        log.info("CBR → INSERT: {} записей сохранено", count);
    }

    // ─────────────────────────── SKIP ─────────────────────────────

    @Test
    @Order(2)
    @DisplayName("CBR → SKIP: второй запуск, данные не изменились")
    void secondRun_shouldSkipAll() {
        producerTemplate.sendBody("direct:triggerSync", null);
        assertThat(partRepository.count()).isEqualTo(TOTAL_CMS_RECORDS);

        Part before = partRepository.findBySpareCode("SPARE-1").orElseThrow();
        LocalDateTime syncedAtBefore = before.getSyncedAt();

        producerTemplate.sendBody("direct:triggerSync", null);

        assertThat(partRepository.count())
                .as("Количество записей не должно измениться")
                .isEqualTo(TOTAL_CMS_RECORDS);

        Part after = partRepository.findBySpareCode("SPARE-1").orElseThrow();

        assertThat(after.getSyncedAt())
                .as("syncedAt не должен измениться при SKIP")
                .isEqualTo(syncedAtBefore);

        log.info("CBR → SKIP: syncedAt не изменился ({})", syncedAtBefore);
    }

    // ─────────────────────────── UPDATE ───────────────────────────

    @Test
    @Order(3)
    @DisplayName("CBR → UPDATE: изменён updatedAt в CMS")
    void changedDate_shouldUpdate() {
        producerTemplate.sendBody("direct:triggerSync", null);

        String staleDate = "2020-09-28T12:35:25.238411853";
        Part part = partRepository.findBySpareCode("SPARE-1").orElseThrow();
        part.setUpdatedAt(staleDate);
        partRepository.save(part);

        producerTemplate.sendBody("direct:triggerSync", null);

        Part updated = partRepository.findBySpareCode("SPARE-1").orElseThrow();

        assertThat(updated.getUpdatedAt())
                .as("updatedAt должен обновиться до значения из CMS")
                .isNotEqualTo(staleDate);

        log.info("CBR → UPDATE: {} → {}", staleDate, updated.getUpdatedAt());
    }

    // ─────────────────────── SOFT DELETE ──────────────────────────

    @Test
    @Order(4)
    @DisplayName("CBR → SOFT DELETE: деталь в Mongo, но нет в CMS")
    void missingInCms_shouldSoftDelete() {
        // Лишняя» деталь, которой нет в CMS
        Part ghost = new Part();
        ghost.setSpareCode("EXTRA-999");
        ghost.setSpareName("Ghost Part");
        ghost.setPrice(BigDecimal.TEN);
        ghost.setQuantity(1);
        ghost.setUpdatedAt("2026-09-28T12:35:25.238411853");
        ghost.setDeleted(false);
        ghost.setSyncedAt(LocalDateTime.now());
        partRepository.save(ghost);

        producerTemplate.sendBody("direct:triggerSync", null);

        Part deleted = partRepository.findBySpareCode("EXTRA-999").orElseThrow();

        assertThat(deleted.isDeleted())
                .as("Деталь, отсутствующая в CMS, должна быть isDeleted=true")
                .isTrue();

        assertThat(deleted.getDeletedAt())
                .as("deletedAt должен быть заполнен")
                .isNotNull();

        assertThat(partRepository.findByIsDeletedFalse())
                .as("Все детали из CMS должны остаться активными")
                .hasSize(TOTAL_CMS_RECORDS);

        log.info("CBR → SOFT DELETE: EXTRA-999 удалён, {} активных", TOTAL_CMS_RECORDS);
    }
}