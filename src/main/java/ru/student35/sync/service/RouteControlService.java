package ru.student35.sync.service;

import org.apache.camel.ProducerTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * EIP: Control Bus.
 *
 * <p>Управление роутами в рантайск через встроенный компонент
 * {@code controlbus:}. Позволяет останавливать и запускать роуты
 * без перезапуска приложения.</p>
 *
 * <p>Примеры использования:</p>
 * <ul>
 *   <li>Остановка синхронизации при техобслуживании CMS</li>
 *   <li>Получение статистики обработанных сообщений</li>
 * </ul>
 */
@Service
public class RouteControlService {

    private static final Logger log = LoggerFactory.getLogger(RouteControlService.class);

    private final ProducerTemplate producerTemplate;

    public RouteControlService(ProducerTemplate producerTemplate) {
        this.producerTemplate = producerTemplate;
    }

    /**
     * Останавливает роут по его ID.
     *
     * @param routeId идентификатор роута (например, "cmsSync")
     */
    public void stopRoute(String routeId) {
        producerTemplate.sendBody(
                "controlbus:route?routeId=" + routeId + "&action=stop",
                null
        );
        log.info("Роут '{}' остановлен через Control Bus", routeId);
    }

    /**
     * Запускает ранее остановленный роут.
     *
     * @param routeId идентификатор роута
     */
    public void startRoute(String routeId) {
        producerTemplate.sendBody(
                "controlbus:route?routeId=" + routeId + "&action=start",
                null
        );
        log.info("Роут '{}' запущен через Control Bus", routeId);
    }

    /**
     * Возвращает текущий статус роута.
     *
     * @param routeId идентификатор роута
     * @return статус (Started / Stopped / Suspended)
     */
    public String getRouteStatus(String routeId) {
        return producerTemplate.requestBody(
                "controlbus:route?routeId=" + routeId + "&action=status",
                null,
                String.class
        );
    }
}