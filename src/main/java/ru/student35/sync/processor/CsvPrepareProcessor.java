package ru.student35.sync.processor;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import ru.student35.sync.model.Part;

import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * EIP: Message Translator (Этап Б).
 *
 * <p>Преобразует List&lt;Part&gt; из MongoDB в CSV-строку
 * для отправки в Report API.</p>
 *
 * <p>Формат: UTF-8, разделитель «;», первая строка — заголовки.</p>
 */
@Component
public class CsvPrepareProcessor implements Processor {

    private static final Logger log = LoggerFactory.getLogger(CsvPrepareProcessor.class);

    private static final String DELIMITER = ";";
    private static final String NEWLINE = "\n";

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS");

    @Override
    @SuppressWarnings("unchecked")
    public void process(Exchange exchange) {
        List<Part> parts = exchange.getIn().getBody(List.class);

        StringBuilder csv = new StringBuilder();

        // Заголовки
        csv.append(String.join(DELIMITER,
                "spareCode", "spareName", "spareDescription", "spareType",
                "spareStatus", "price", "quantity", "updatedAt"
        )).append(NEWLINE);

        // Данные
        if (parts != null) {
            for (Part part : parts) {
                csv.append(toRow(part)).append(NEWLINE);
            }
        }

        log.info("CSV сформирован: {} строк, {} байт",
                parts != null ? parts.size() : 0, csv.length());

        exchange.getIn().setBody(csv.toString());
    }

    private String toRow(Part part) {
        String date = "";
        if (part.getUpdatedAt() != null) {
            date = part.getUpdatedAt().atStartOfDay().format(DATE_FORMATTER);
        }

        String price = part.getPrice() != null
                ? part.getPrice().stripTrailingZeros().toPlainString()
                : "0";

        return String.join(DELIMITER,
                nullSafe(part.getSpareCode()),
                nullSafe(part.getSpareName()),
                nullSafe(part.getSpareDescription()),
                nullSafe(part.getSpareType()),
                nullSafe(part.getSpareStatus()),
                price,
                part.getQuantity() != null ? part.getQuantity().toString() : "0",
                date
        );
    }

    private String nullSafe(String s) {
        return s != null ? s : "";
    }
}