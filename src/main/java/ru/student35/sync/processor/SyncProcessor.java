package ru.student35.sync.processor;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import ru.student35.sync.model.Part;
import ru.student35.sync.repository.PartRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * EIP: Content-Based Router.
 *
 * <p>Маршрутизирует каждую запись на основе сравнения с данными в MongoDB:</p>
 * <ul>
 *   <li><b>INSERT</b> — spareCode не найден в БД</li>
 *   <li><b>UPDATE</b> — spareCode найден, updatedAt изменился</li>
 *   <li><b>SKIP</b> — spareCode найден, updatedAt тот же</li>
 *   <li><b>SOFT DELETE</b> — запись есть в БД, но отсутствует в CMS</li>
 * </ul>
 */
@Component
public class SyncProcessor implements Processor {

    private static final Logger log = LoggerFactory.getLogger(SyncProcessor.class);

    private final PartRepository partRepository;

    public SyncProcessor(PartRepository partRepository) {
        this.partRepository = partRepository;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void process(Exchange exchange) {
        List<Part> parts = exchange.getIn().getBody(List.class);
        Set<String> cmsCodes = parts.stream()
                .map(Part::getSpareCode)
                .collect(Collectors.toSet());

        int inserted = 0, updated = 0, skipped = 0;

        for (Part part : parts) {
            Optional<Part> existing = partRepository.findBySpareCode(part.getSpareCode());

            if (existing.isEmpty()) {
                // Ветка INSERT
                partRepository.save(part);
                inserted++;
            } else if (isChanged(existing.get(), part)) {
                // Ветка UPDATE
                part.setId(existing.get().getId());
                partRepository.save(part);
                updated++;
            } else {
                // Ветка SKIP
                skipped++;
            }
        }

        // Ветка SOFT DELETE
        int deleted = softDeleteMissing(cmsCodes);

        log.info("CBR: +{} insert, ~{} update, ={} skip, -{} softDelete",
                inserted, updated, skipped, deleted);

        exchange.getIn().setHeader("inserted", inserted);
        exchange.getIn().setHeader("updated", updated);
        exchange.getIn().setHeader("skipped", skipped);
        exchange.getIn().setHeader("deleted", deleted);
    }

    private boolean isChanged(Part existing, Part incoming) {
        if (incoming.getUpdatedAt() == null) return false;
        return !incoming.getUpdatedAt().equals(existing.getUpdatedAt());
    }

    private int softDeleteMissing(Set<String> cmsCodes) {
        List<Part> active = partRepository.findByIsDeletedFalse();
        int count = 0;

        for (Part part : active) {
            if (!cmsCodes.contains(part.getSpareCode())) {
                part.setDeleted(true);
                part.setDeletedAt(LocalDateTime.now());
                part.setSyncedAt(LocalDateTime.now());
                partRepository.save(part);
                count++;
            }
        }

        return count;
    }
}