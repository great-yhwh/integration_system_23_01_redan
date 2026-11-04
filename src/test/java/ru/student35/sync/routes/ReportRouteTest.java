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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Интеграционный тест Потока Б: MongoDB → CSV → Report API.
 *
 * <p>Проверяет работу Message Translator и Point-to-Point Channel.</p>
 */
@CamelSpringBootTest
@SpringBootTest
@UseAdviceWith
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Интеграционное тестирование роута: MongoDB → CSV → Report API")
class ReportRouteTest {

    private static final Logger log = LoggerFactory.getLogger(ReportRouteTest.class);

    private static Process mockProcess;

    @Autowired
    private CamelContext camelContext;

    @Autowired
    private ProducerTemplate producerTemplate;

    @Autowired
    private PartRepository partRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

    // ──────────────────────── Mock ────────────────────────

    @BeforeAll
    @DisplayName("Запуск мок-сервера Report API")
    static void startMock() throws Exception {
        ProcessBuilder pb = new ProcessBuilder("node", "src/test/resources/mocks/report-mock.js");
        pb.redirectErrorStream(true);
        mockProcess = pb.start();
        Thread.sleep(1000);

        assertThat(mockProcess.isAlive())
                .as("Мок-сервер Report API должен успешно запуститься")
                .isTrue();
    }

    @AfterAll
    @DisplayName("Остановка мок-сервера Report API")
    static void stopMock() {
        if (mockProcess != null) {
            mockProcess.destroyForcibly();
        }
    }

    private static boolean adviceApplied = false;

    @BeforeEach
    @DisplayName("Очистка БД, сидирование данных и подготовка роута")
    void setUp() throws Exception {
        mongoTemplate.dropCollection(Part.class);
        seedData();

        if (!adviceApplied) {
            AdviceWith.adviceWith(camelContext, "reportSync", a ->
                    a.replaceFromWith("direct:triggerReport")
            );
            adviceApplied = true;
        }

        if (!camelContext.isStarted()) {
            camelContext.start();
        }
    }


    @Test
    @Order(1)
    @DisplayName("Поток Б: CSV содержит заголовки и активные записи")
    void reportRoute_shouldContainHeadersAndActiveParts() throws Exception {
        producerTemplate.sendBody("direct:triggerReport", null);
        Thread.sleep(500);

        String response = httpGet("http://localhost:4501/report/last");

        assertThat(response)
                .as("Мок должен подтвердить получение CSV")
                .contains("\"bodyLength\"");

        assertThat(response)
                .as("CSV должен содержать заголовки через ;")
                .contains("spareCode;spareName");

        assertThat(response)
                .as("CSV должен содержать активную деталь из MongoDB")
                .contains("SPARE-TEST-1");

        log.info("Report Mock ответил: {}", response.substring(0, Math.min(200, response.length())));
    }

    @Test
    @Order(2)
    @DisplayName("Поток Б: CSV включает удалённые записи (историчность)")
    void reportRoute_shouldIncludeDeletedParts() throws Exception {
        producerTemplate.sendBody("direct:triggerReport", null);
        Thread.sleep(500);

        String response = httpGet("http://localhost:4501/report/last");

        assertThat(response)
                .as("CSV должен включать даже удалённые записи")
                .contains("SPARE-DELETED");

        log.info("Удалённая запись SPARE-DELETED присутствует в отчёте");
    }


    private void seedData() {
        Part active = new Part();
        active.setSpareCode("SPARE-TEST-1");
        active.setSpareName("Test Part");
        active.setPrice(new BigDecimal("10.50"));
        active.setQuantity(5);
        active.setUpdatedAt(LocalDate.of(2025, 1, 15));
        active.setDeleted(false);
        active.setSyncedAt(LocalDateTime.now());
        partRepository.save(active);

        Part deleted = new Part();
        deleted.setSpareCode("SPARE-DELETED");
        deleted.setSpareName("Old Part");
        deleted.setPrice(new BigDecimal("5"));
        deleted.setQuantity(0);
        deleted.setUpdatedAt(LocalDate.of(2024, 6, 1));
        deleted.setDeleted(true);
        deleted.setDeletedAt(LocalDateTime.now());
        deleted.setSyncedAt(LocalDateTime.now());
        partRepository.save(deleted);
    }

    private String httpGet(String urlStr) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
        conn.setRequestMethod("GET");

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream()))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
            return sb.toString();
        }
    }
}