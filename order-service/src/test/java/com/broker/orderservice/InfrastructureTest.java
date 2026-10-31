package com.broker.orderservice;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.micrometer.core.instrument.MeterRegistry;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.lifecycle.Startables;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class InfrastructureTest {

    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");
    static final GenericContainer<?> redis = new GenericContainer<>("redis:7").withExposedPorts(6379);
    static final KafkaContainer kafka = new KafkaContainer("apache/kafka:3.8.0");

    static {
        Startables.deepStart(postgres, redis, kafka).join();
    }

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", postgres::getJdbcUrl);
        r.add("spring.datasource.username", postgres::getUsername);
        r.add("spring.datasource.password", postgres::getPassword);
        r.add("spring.data.redis.host", redis::getHost);
        r.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
        r.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }

    @Autowired TestRestTemplate rest;
    @Autowired MeterRegistry registry;
    @Autowired OrderConsumer consumer;

    @Test
    void healthIsUpWithAllDependencies() {
        var res = rest.getForEntity("/actuator/health", String.class);
        assertEquals(200, res.getStatusCode().value());
        assertTrue(res.getBody().contains("\"status\":\"UP\""));
    }

    @Test
    void orderFlowsThroughKafka() {
        var res = rest.postForEntity("/orders/AAPL?action=buy", null, String.class);
        assertEquals(200, res.getStatusCode().value());
        assertEquals("queued", res.getBody());

        await().atMost(Duration.ofSeconds(90)).pollInterval(Duration.ofMillis(500))
                .untilAsserted(() -> assertTrue(consumer.getCount() >= 1));
    }
}