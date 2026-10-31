package com.broker.orderservice;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopics {
    @Bean
    NewTopic orders() {
        return TopicBuilder.name("orders").partitions(3).replicas(1).build();
    }
}