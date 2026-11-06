package ru.student35.sync.routes;

import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;
import ru.student35.sync.processor.SpareToPartTranslator;
import ru.student35.sync.processor.SyncProcessor;
import ru.student35.sync.service.CmsClient;

/**
 * Роут синхронизации CMS → MongoDB (Поток А).
 *
 * <ul>
 *   <li><b>Polling Consumer</b> — timer инициирует опрос CMS</li>
 *   <li><b>Document Message</b> — CMS возвращает JSON</li>
 *   <li><b>Point-to-Point Channel</b> — каналы с одним получателем</li>
 *   <li><b>Message Translator</b> — SpareToPartTranslator</li>
 *   <li><b>Content-Based Router</b> — SyncProcessor</li>
 * </ul>
 *
 * <p>Timer → HTTP → Translator → MongoDB</p>
 */
@Component
public class CmsSyncRoute extends RouteBuilder {

    private final CmsClient cmsClient;
    private final SpareToPartTranslator translator;
    private final SyncProcessor syncProcessor;

    public CmsSyncRoute(CmsClient cmsClient,
                        SpareToPartTranslator translator,
                        SyncProcessor syncProcessor) {
        this.cmsClient = cmsClient;
        this.translator = translator;
        this.syncProcessor = syncProcessor;
    }

    @Override
    public void configure() {

        from("timer:cmsSync?period={{sync.interval}}")
                .routeId("cmsSync")
                .log("=== Запуск синхронизации ===")
                .to("direct:syncCms")
                .to("direct:generateReport");

        from("direct:syncCms")
                .routeId("syncCms")
                .bean(cmsClient, "fetchAllSpares")
                .log("Получено из CMS: ${body.size()} записей")
                .process(translator)
                .process(syncProcessor)
                .log("CBR результат: +${header.inserted} ~${header.updated} " +
                        "=${header.skipped} -${header.deleted}");
    }
}