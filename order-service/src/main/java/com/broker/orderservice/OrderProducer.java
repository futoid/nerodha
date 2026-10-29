package com.broker.orderservice;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;

@RestController
public class OrderProducer {
    private final KafkaTemplate<String, String> kafka;

    public OrderProducer(KafkaTemplate<String, String> kafka) {
        this.kafka = kafka;
    }

    @PostMapping("/orders/{symbol}")
    public String send(@PathVariable String symbol, @RequestParam String action) {
        kafka.send("orders", symbol, action + " " + symbol)
                .whenComplete((r, ex) -> {
                    if (ex != null) System.err.println("send failed: " + ex);
                    else System.out.println("sent to partition " + r.getRecordMetadata().partition()
                            + " offset " + r.getRecordMetadata().offset());
                });
        return "queued";
    }
}