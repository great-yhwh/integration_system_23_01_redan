package ru.student35.sync.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ru.student35.sync.config.CmsProperties;
import ru.student35.sync.dto.cms.SpareDto;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * Клиент для получения данных из CMS API с пагинацией и Basic Auth.
 *
 * <p>Используется в роуте через {@code .bean(cmsClient)}.</p>
 */
@Service
public class CmsClient {

    private static final Logger log = LoggerFactory.getLogger(CmsClient.class);

    private final CmsProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public CmsClient(CmsProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newHttpClient();
    }

    /**
     * Загружает все детали из CMS, обходя все страницы пагинации.
     * Вызывается из роута через .bean()
     */
    public List<SpareDto> fetchAllSpares() throws Exception {
        List<SpareDto> all = new ArrayList<>();
        int page = 0;
        boolean hasMore = true;

        while (hasMore) {
            List<SpareDto> pageData = fetchPage(page);
            all.addAll(pageData);
            hasMore = pageData.size() == properties.getPageSize();
            page++;
        }

        log.info("Загружено {} деталей из CMS ({} страниц)", all.size(), page);
        return all;
    }

    private List<SpareDto> fetchPage(int page) throws Exception {
        String url = String.format("%s/spares?page=%d&size=%d",
                properties.getBaseUrl(), page, properties.getPageSize());

        String credentials = properties.getUsername() + ":" + properties.getPassword();
        String auth = "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes());

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", auth)
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("CMS вернул " + response.statusCode());
        }

        return objectMapper.readValue(response.body(), new TypeReference<>() {});
    }
}