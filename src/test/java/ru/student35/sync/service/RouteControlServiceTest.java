package ru.student35.sync.service;

import org.apache.camel.CamelContext;
import org.apache.camel.ServiceStatus;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Тест EIP Control Bus.
 *
 * <p>Проверяет возможность управлять роутами в рантайме
 * без перезапуска приложения.</p>
 */
@CamelSpringBootTest
@SpringBootTest
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Интеграционное тестирование EIP Control Bus")
class RouteControlServiceTest {

    private static final Logger log = LoggerFactory.getLogger(RouteControlServiceTest.class);

    private static final String ROUTE_ID = "cmsSync";

    @Autowired
    private RouteControlService controlService;

    @Autowired
    private CamelContext camelContext;

    @Test
    @Order(1)
    @DisplayName("Control Bus: stop → статус Stopped")
    void shouldStopRoute() throws Exception {
        assertThat(camelContext.getRouteController().getRouteStatus(ROUTE_ID))
                .as("Роут должен быть запущен до остановки")
                .isEqualTo(ServiceStatus.Started);

        controlService.stopRoute(ROUTE_ID);
        Thread.sleep(500);

        assertThat(camelContext.getRouteController().getRouteStatus(ROUTE_ID))
                .as("После stopRoute() роут должен быть Stopped")
                .isEqualTo(ServiceStatus.Stopped);

        assertThat(controlService.getRouteStatus(ROUTE_ID))
                .as("getRouteStatus() должен вернуть строку 'Stopped'")
                .isEqualTo("Stopped");

        log.info("Control Bus: роут {} остановлен", ROUTE_ID);
    }

    @Test
    @Order(2)
    @DisplayName("Control Bus: start → статус Started")
    void shouldStartRoute() throws Exception {
        controlService.startRoute(ROUTE_ID);
        Thread.sleep(500);

        assertThat(camelContext.getRouteController().getRouteStatus(ROUTE_ID))
                .as("После startRoute() роут должен быть Started")
                .isEqualTo(ServiceStatus.Started);

        log.info("Control Bus: роут {} запущен", ROUTE_ID);
    }
}