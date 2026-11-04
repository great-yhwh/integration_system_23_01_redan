package ru.student35.sync.routes;

import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;
import ru.student35.sync.config.CmsProperties;
import ru.student35.sync.model.Part;
import ru.student35.sync.processor.SpareToPartTranslator;
import ru.student35.sync.repository.PartRepository;

import java.util.Base64;
import java.util.List;

/**
 * Роут синхронизации CMS → MongoDB (Поток А).
 *
 * <ul>
 *   <li><b>Polling Consumer</b> — timer инициирует опрос CMS</li>
 *   <li><b>Document Message</b> — CMS возвращает JSON</li>
 *   <li><b>Point-to-Point Channel</b> — канал с одним получателем</li>
 *   <li><b>Message Translator</b> — SpareToPartTranslator</li>
 * </ul>
 *
 * <p>Timer → HTTP → Translator → MongoDB</p>
 */
@Component
public class CmsSyncRoute extends RouteBuilder {

    private final CmsProperties cmsProperties;
    private final SpareToPartTranslator translator;
    private final PartRepository partRepository;

    public CmsSyncRoute(CmsProperties cmsProperties,
                        SpareToPartTranslator translator,
                        PartRepository partRepository) {
        this.cmsProperties = cmsProperties;
        this.translator = translator;
        this.partRepository = partRepository;
    }

    @Override
    public void configure() {

        from("timer:cmsSync?period={{sync.interval}}")
                .routeId("cmsSync")
                .log("=== Запуск синхронизации с CMS ===")
                .to("direct:fetchFromCms");

        from("direct:fetchFromCms")
                .routeId("fetchFromCms")
                .setHeader("Authorization", constant(basicAuthHeader()))
                .setHeader("CamelHttpMethod", constant("GET"))
                .toD(cmsProperties.getBaseUrl() + "/spares?page=0&size=" + cmsProperties.getPageSize())
                .convertBodyTo(String.class)
                .log("Получен ответ от CMS: ${body.length()} байт")
                .process(translator)
                .to("direct:saveToMongo");

        from("direct:saveToMongo")
                .routeId("saveToMongo")
                .process(exchange -> {
                    @SuppressWarnings("unchecked")
                    List<Part> parts = exchange.getIn().getBody(List.class);
                    partRepository.saveAll(parts);
                    exchange.getIn().setHeader("savedCount", parts.size());
                })
                .log("Сохранено в MongoDB: ${header.savedCount} записей");
    }

    private String basicAuthHeader() {
        String credentials = cmsProperties.getUsername() + ":" + cmsProperties.getPassword();
        return "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes());
    }
}