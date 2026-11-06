package ru.student35.sync.routes;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;
import ru.student35.sync.processor.CsvPrepareProcessor;
import ru.student35.sync.repository.PartRepository;
import ru.student35.sync.service.ReportAuthClient;

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
    private final ReportAuthClient reportAuthClient; 

    public ReportRoute(PartRepository partRepository,
                       CsvPrepareProcessor csvPrepareProcessor,
                       ReportAuthClient reportAuthClient) {
        this.partRepository = partRepository;
        this.csvPrepareProcessor = csvPrepareProcessor;
        this.reportAuthClient = reportAuthClient;
    }

    @Override
    public void configure() {

        from("direct:generateReport")
                .routeId("generateReport")
                .process(exchange -> exchange.getMessage().setBody(partRepository.findAll()))
                .process(csvPrepareProcessor)
                .bean(reportAuthClient, "addTokenHeader")
                .setHeader(Exchange.HTTP_METHOD, constant("POST"))
                .setHeader(Exchange.CONTENT_TYPE, constant("text/csv"))
                .process(exchange -> {
                    String csv = exchange.getMessage().getBody(String.class);
                    log.info("CSV первая строка: {}", csv.split("\n")[0]);
                })
                .to("{{report.api-url}}")
                .log("Отчёт отправлен: ${body}");
    }
}