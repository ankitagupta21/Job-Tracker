package com.jobtracker.backend.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

import com.jobtracker.backend.constants.KafkaTopics;

@Configuration
public class KafkaConfig {

    @Bean
    public NewTopic applicationCreated() {
        return TopicBuilder.name(KafkaTopics.APPLICATION_CREATED).partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic applicationInterview() {
        return TopicBuilder.name(KafkaTopics.APPLICATION_INTERVIEW).partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic applicationRejected() {
        return TopicBuilder.name(KafkaTopics.APPLICATION_REJECTED).partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic applicationOffered() {
        return TopicBuilder.name(KafkaTopics.APPLICATION_OFFERED).partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic applicationOa() {
        return TopicBuilder.name(KafkaTopics.APPLICATION_OA).partitions(1).replicas(1).build();
    }
}