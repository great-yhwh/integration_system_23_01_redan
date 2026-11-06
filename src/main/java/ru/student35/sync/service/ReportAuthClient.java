package ru.student35.sync.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.camel.Exchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;

@Service
public class ReportAuthClient {

    private static final Logger log = LoggerFactory.getLogger(ReportAuthClient.class);
    private static final long MARGIN_SECONDS = 60;

    private final String tokenUrl;
    private final String jsonBody;
    private final HttpClient httpClient;
    private final ObjectMapper mapper;

    private String cachedToken;
    private long validUntil;

    public ReportAuthClient(
            @Value("${auth.token-url}") String tokenUrl,
            @Value("${cms.username}") String username,
            @Value("${cms.password}") String password) {
        this.tokenUrl = tokenUrl;
        this.jsonBody = String.format(
                "{\"username\":\"%s\",\"password\":\"%s\"}", username, password);
        this.httpClient = HttpClient.newHttpClient();
        this.mapper = new ObjectMapper();
    }

    public synchronized String getToken() {
        long now = Instant.now().getEpochSecond();
        if (cachedToken == null || now >= validUntil - MARGIN_SECONDS) {
            refreshToken();
        }
        return cachedToken;
    }

    private void refreshToken() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(tokenUrl))
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .header("Content-Type", "application/json")
                    .header("Accept", "*/*")
                    .build();

            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new RuntimeException("Auth вернул " + response.statusCode()
                        + ": " + response.body());
            }

            JsonNode json = mapper.readTree(response.body());
            this.cachedToken = json.get("token").asText();
            this.validUntil = json.get("validUntil").asLong();

            log.info("Токен получен, действителен до {}", Instant.ofEpochSecond(validUntil));

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Не удалось получить токен", e);
        }
    }

    /** Вызывается из Camel-роута для добавления заголовка */
    public void addTokenHeader(Exchange exchange) {
        exchange.getMessage().setHeader("Authorization", "Bearer " + getToken());
    }
}