package ru.student35.sync.processor;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.stereotype.Component;
import ru.student35.sync.model.Part;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Component
public class CsvPrepareProcessor implements Processor {

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS");

    @Override
    public void process(Exchange exchange) {
        /* Преобразует список сущностей Part из БД в строки для CSV-файла */
        @SuppressWarnings("unchecked")
        List<Part> parts = exchange.getIn().getBody(List.class);

        List<String[]> csvRows = new ArrayList<>();
        csvRows.add(new String[]{
                "spareCode", "spareName", "spareDescription", "spareType",
                "spareStatus", "price", "quantity", "updatedAt"
        });

        if (parts != null) {
            for (Part part : parts) {
                String formattedDate = "";
                if (part.getUpdatedAt() != null) {
                    formattedDate = part.getUpdatedAt().atStartOfDay().format(DATE_FORMATTER);
                }

                String formattedPrice = part.getPrice() != null
                        ? part.getPrice().stripTrailingZeros().toPlainString()
                        : "0";

                csvRows.add(new String[]{
                        part.getSpareCode(),
                        part.getSpareName(),
                        part.getSpareDescription(),
                        part.getSpareType(),
                        part.getSpareStatus(),
                        formattedPrice,
                        part.getQuantity() != null ? part.getQuantity().toString() : "0",
                        formattedDate
                });
            }
        }

        exchange.getIn().setBody(csvRows);
    }
}