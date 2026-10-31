package com.broker.orderservice;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class OrderConsumer {
    private final Counter consumed;
    private final AtomicLong count = new AtomicLong();

    public long getCount() { return count.get(); }

    public OrderConsumer(MeterRegistry registry) {
        this.consumed = Counter.builder("orders_consumed_total")
                .description("Orders read from Kafka")
                .register(registry);
    }
    @KafkaListener(topics = "orders")
    public void onOrder(ConsumerRecord<String, String> rec) {
        count.incrementAndGet();
        System.out.printf("[%s] partition=%d offset=%d key=%s value=%s%n",
                Thread.currentThread().getName(), rec.partition(), rec.offset(), rec.key(), rec.value());
    }
}