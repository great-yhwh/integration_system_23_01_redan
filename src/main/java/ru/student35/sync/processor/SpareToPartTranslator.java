package ru.student35.sync.processor;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import ru.student35.sync.dto.cms.SpareDto;
import ru.student35.sync.model.Part;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * EIP: Message Translator.
 *
 * <p>Преобразует список {@link SpareDto} из CMS в список {@link Part}
 * для сохранения в MongoDB.</p>
 */
@Component
public class SpareToPartTranslator implements Processor {

    private static final Logger log = LoggerFactory.getLogger(SpareToPartTranslator.class);

    @Override
    @SuppressWarnings("unchecked")
    public void process(Exchange exchange) {
        // CmsClient уже вернул List<SpareDto> — десериализация не нужна
        List<SpareDto> dtos = exchange.getIn().getBody(List.class);

        if (dtos == null) {
            log.warn("Трансляция: тело пустое");
            exchange.getIn().setBody(List.of());
            return;
        }

        List<Part> parts = dtos.stream()
                .map(this::toPart)
                .collect(Collectors.toList());

        log.info("Трансляция: получено {} DTO, преобразовано в {} Part", dtos.size(), parts.size());

        exchange.getIn().setBody(parts);
    }

    private Part toPart(SpareDto dto) {
        Part part = new Part();
        part.setSpareCode(dto.getSpareCode());
        part.setSpareName(dto.getSpareName());
        part.setSpareDescription(dto.getSpareDescription());
        part.setSpareType(dto.getSpareType());
        part.setSpareStatus(dto.getSpareStatus());
        part.setQuantity(dto.getQuantity());

        part.setPrice(parsePrice(dto.getPrice()));
        part.setUpdatedAt(parseDate(dto.getUpdatedAt()));

        part.setDeleted(false);
        part.setSyncedAt(LocalDateTime.now());

        return part;
    }

    private BigDecimal parsePrice(String price) {
        if (price == null || price.isBlank()) return BigDecimal.ZERO;
        try {
            return new BigDecimal(price);
        } catch (NumberFormatException e) {
            log.warn("Невозможно распарсить цену: {}", price);
            return BigDecimal.ZERO;
        }
    }

    private LocalDate parseDate(String date) {
        if (date == null || date.isBlank()) return null;
        try {
            return LocalDateTime.parse(date).toLocalDate();
        } catch (Exception e) {
            log.warn("Невозможно распарсить дату: {}", date);
            return null;
        }
    }
}