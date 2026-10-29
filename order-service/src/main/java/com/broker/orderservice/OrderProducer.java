package com.broker.orderservice;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

@RestController
public class OrderProducer {
    private final KafkaTemplate<String, String> kafka;
    private final Counter ordersSent;

    public OrderProducer(KafkaTemplate<String, String> kafka, MeterRegistry registry) {
        this.kafka = kafka;
        this.ordersSent = Counter.builder("orders_sent_total")
                .description("Orders published to Kafka")
                .register(registry);
    }

    @PostMapping("/orders/{symbol}")
    public String send(@PathVariable String symbol, @RequestParam String action) {
        ordersSent.increment();
        kafka.send("orders", symbol, action + " " + symbol)
                .whenComplete((r, ex) -> {
                    if (ex != null) System.err.println("send failed: " + ex);
                    else System.out.println("sent to partition " + r.getRecordMetadata().partition()
                            + " offset " + r.getRecordMetadata().offset());
                });
        return "queued";
    }
}