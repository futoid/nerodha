package com.broker.orderservice;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class OrderConsumer {
    @KafkaListener(topics = "orders")
    public void onOrder(ConsumerRecord<String, String> rec) {
        System.out.printf("[%s] partition=%d offset=%d key=%s value=%s%n",
                Thread.currentThread().getName(), rec.partition(), rec.offset(), rec.key(), rec.value());
    }
}