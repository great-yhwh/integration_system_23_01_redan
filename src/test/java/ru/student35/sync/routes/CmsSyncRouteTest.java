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
import ru.student35.sync.config.CmsProperties;
import ru.student35.sync.model.Part;
import ru.student35.sync.repository.PartRepository;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Интеграционный тест роута Поток А: CMS → MongoDB.
 *
 * <p>Проверяет работу полного конвейера:</p>
 * <ol>
 *   <li>Timer срабатывает (заменён на ручной запуск)</li>
 *   <li>HTTP-запрос к CMS с Basic Auth</li>
 *   <li>Message Translator преобразует SpareDto в Part</li>
 *   <li>Данные сохраняются в MongoDB</li>
 * </ol>
 */
@CamelSpringBootTest
@SpringBootTest
@UseAdviceWith
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("Интеграционное тестирование роута: CMS → MongoDB")
class CmsSyncRouteTest {
    private static final Logger log = LoggerFactory.getLogger(CmsSyncRouteTest.class);

    private static Process mockProcess;

    @Autowired
    private CamelContext camelContext;

    @Autowired
    private ProducerTemplate producerTemplate;

    @Autowired
    private PartRepository partRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private CmsProperties cmsProperties;

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

    @BeforeEach
    @DisplayName("Очистка БД и подготовка роута")
    void setUp() throws Exception {
        mongoTemplate.dropCollection(Part.class);

        AdviceWith.adviceWith(camelContext, "cmsSync", a ->
                a.replaceFromWith("direct:triggerSync")
        );

        camelContext.start();
    }

    @Test
    @DisplayName("Camel-роут: Timer → HTTP → Translator → MongoDB")
    void cmsSyncRoute_shouldFetchTranslateAndSave() {
        producerTemplate.sendBody("direct:triggerSync", null);

        int expectedSize = cmsProperties.getPageSize();
        long count = partRepository.count();

        assertThat(count)
                .as("Количество записей в БД должно совпадать с page-size")
                .isEqualTo(expectedSize);

        Optional<Part> found = partRepository.findBySpareCode("SPARE-1");

        assertThat(found)
                .as("SPARE-1 должна быть сохранена в БД")
                .isPresent();

        Part part = found.get();

        assertThat(part.getId())
                .as("MongoDB должна сгенерировать ID")
                .isNotNull();

        assertThat(part.getSpareName()).isEqualTo("Spare Part 1");

        assertThat(part.getPrice())
                .as("Цена должна быть BigDecimal")
                .isInstanceOf(BigDecimal.class);

        assertThat(part.getUpdatedAt())
                .as("Дата должна быть распарсена")
                .isNotNull();

        assertThat(part.isDeleted())
                .as("Новая деталь не должна быть помечена как удалённая")
                .isFalse();

        assertThat(part.getSyncedAt())
                .as("syncedAt должен быть заполнен")
                .isNotNull();

        log.info("Роут отработал успешно");
        log.info("  Записей в БД:    {}", count);
        log.info("  spareCode:       {}", part.getSpareCode());
        log.info("  spareName:       {}", part.getSpareName());
        log.info("  price:           {} ({})", part.getPrice(), part.getPrice().getClass().getSimpleName());
        log.info("  updatedAt:       {}", part.getUpdatedAt());
        log.info("  syncedAt:        {}", part.getSyncedAt());
    }
}