package ru.student35.sync.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.student35.sync.config.CmsProperties;
import ru.student35.sync.dto.cms.SpareDto;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("Интеграционное тестирование интеграции с API CMS (через HTTP-клиент)")
class CmsClientTest {
    private static final Logger log = LoggerFactory.getLogger(CmsClientTest.class);

    private static final int PORT = 4500;
    private static Process mockProcess;

    @Autowired
    private CmsProperties cmsProperties;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeAll
    @DisplayName("Запуск мок-сервера CMS")
    static void startMock() throws Exception {
        ProcessBuilder pb = new ProcessBuilder("node", "src/test/resources/mocks/cms-mock.js");
        pb.environment().put("PORT", String.valueOf(PORT));
        pb.redirectErrorStream(true);

        mockProcess = pb.start();
        Thread.sleep(800);

        assertThat(mockProcess.isAlive())
                .as("Мок-сервер CMS должен успешно запуститься на порту " + PORT)
                .isTrue();
    }

    @AfterAll
    @DisplayName("Остановка мок-сервера CMS")
    static void stopMock() {
        if (mockProcess != null) {
            mockProcess.destroyForcibly();
        }
    }

    private String getSparesUrl() {
        return cmsProperties.getBaseUrl() + "/spares";
    }

    private String getBasicAuthHeader(String username, String password) {
        String credentials = username + ":" + password;
        return "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes());
    }

    private void logHttpExchange(HttpResponse<String> response) {
        log.info("Получен статус-код ответа: {}", response.statusCode());
        log.info("Тело ответа: {}", response.body());
    }

    @Test
    @DisplayName("Успешный запрос с BasicAuth")
    void fetchSpares_withValidAuth_shouldReturn200() throws Exception {
        String url = getSparesUrl() + "?page=0&size=" + cmsProperties.getPageSize();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", getBasicAuthHeader(cmsProperties.getUsername(), cmsProperties.getPassword()))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        logHttpExchange(response);

        assertThat(response.statusCode()).isEqualTo(200);
    }

    @Test
    @DisplayName("Ответ CMS парсится в DTO")
    void fetchSpares_shouldCorrectlyParseJsonToDto() throws Exception {
        int expectedSize = cmsProperties.getPageSize();
        String url = getSparesUrl() + "?page=0&size=" + expectedSize;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", getBasicAuthHeader(cmsProperties.getUsername(), cmsProperties.getPassword()))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        logHttpExchange(response);

        List<SpareDto> spares = objectMapper.readValue(
                response.body(),
                new TypeReference<>() {}
        );

        log.info("Десериализовано деталей в список: {}", spares.size());

        assertThat(spares)
                .isNotEmpty()
                .hasSize(expectedSize);

        SpareDto firstSpare = spares.get(0);

        log.info("Поля первой детали в списке ---");
        log.info("  spareCode:        {}", firstSpare.getSpareCode());
        log.info("  spareName:        {}", firstSpare.getSpareName());
        log.info("  spareDescription: {}", firstSpare.getSpareDescription());
        log.info("  spareType:        {}", firstSpare.getSpareType());
        log.info("  spareStatus:      {}", firstSpare.getSpareStatus());
        log.info("  price:            {}", firstSpare.getPrice());
        log.info("  quantity:         {}", firstSpare.getQuantity());
        log.info("  updatedAt:        {}", firstSpare.getUpdatedAt());

        assertThat(firstSpare.getSpareCode()).isNotNull().startsWith("SPARE-");
        assertThat(firstSpare.getSpareName()).isNotNull().contains("Spare Part");
        assertThat(firstSpare.getPrice()).isNotNull();
        assertThat(firstSpare.getUpdatedAt()).isNotNull();
    }


    @Test
    @DisplayName("Запрос без заголовка авторизации")
    void fetchSpares_withoutAuth_shouldReturn401() throws Exception {
        String url = getSparesUrl();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        logHttpExchange(response);

        assertThat(response.statusCode()).isEqualTo(401);
    }

    @Test
    @DisplayName("Запрос с некорректными логином или паролем")
    void fetchSpares_withInvalidAuth_shouldReturn401() throws Exception {
        String url = getSparesUrl();
        String wrongUser = "wrong_user";
        String wrongPassword = "wrong_password";

        log.info("Некорректные данные [{}:{}]", wrongUser, wrongPassword);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", getBasicAuthHeader(wrongUser, wrongPassword))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        logHttpExchange(response);


        assertThat(response.statusCode()).isEqualTo(401);
    }
}