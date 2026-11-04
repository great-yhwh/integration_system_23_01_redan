package ru.student35.sync.routes;

import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;
import ru.student35.sync.processor.CsvPrepareProcessor;
import ru.student35.sync.repository.PartRepository;

/**
 * Роут формирования отчёта: MongoDB → CSV → Report API (Поток Б).
 *
 * <ul>
 *   <li><b>Polling Consumer</b> — timer:reportSync</li>
 *   <li><b>Message Translator</b> — CsvPrepareProcessor (Part → CSV)</li>
 *   <li><b>Point-to-Point Channel</b> — direct:generateReport</li>
 * </ul>
 */
@Component
public class ReportRoute extends RouteBuilder {

    private final PartRepository partRepository;
    private final CsvPrepareProcessor csvPrepareProcessor;

    public ReportRoute(PartRepository partRepository,
                       CsvPrepareProcessor csvPrepareProcessor) {
        this.partRepository = partRepository;
        this.csvPrepareProcessor = csvPrepareProcessor;
    }

    @Override
    public void configure() {

        from("timer:reportSync?period={{report-sync.interval}}")
                .routeId("reportSync")
                .log("=== Запуск формирования отчёта ===")
                .to("direct:generateReport");

        from("direct:generateReport")
                .routeId("generateReport")
                .bean(partRepository, "findAllByOrderBySpareCodeAsc")
                .log("Извлечено из MongoDB: ${body.size()} записей")
                .process(csvPrepareProcessor)
                .setHeader("Content-Type", constant("text/csv; charset=UTF-8"))
                .setHeader("CamelHttpMethod", constant("POST"))
                .toD("{{report.api-url}}/upload")
                .log("Отчёт отправлен, ответ: ${body}");
    }
}