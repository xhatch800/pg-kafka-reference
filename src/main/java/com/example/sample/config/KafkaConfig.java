package com.example.sample.config;

import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
public class KafkaConfig {

    public static final String DLT_SUFFIX = ".DLT";

    @Bean
    public NewTopic employeeTerminationsTopic(@Value("${app.kafka.topics.employee-terminations}") String topic) {
        return TopicBuilder.name(topic).partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic employeeTerminationsDltTopic(@Value("${app.kafka.topics.employee-terminations}") String topic) {
        return TopicBuilder.name(topic + DLT_SUFFIX).partitions(1).replicas(1).build();
    }

    /**
     * Retries a failed record 3 times, 1 second apart, then publishes it to {@code <topic>.DLT}.
     * Records that could not be deserialized arrive as raw bytes, so they are re-published with a
     * byte-array serializer; everything else goes through the default JSON template.
     */
    @Bean
    public DefaultErrorHandler kafkaErrorHandler(
            KafkaTemplate<Object, Object> kafkaTemplate, ProducerFactory<Object, Object> producerFactory) {
        KafkaTemplate<Object, Object> bytesTemplate = new KafkaTemplate<>(
                producerFactory, Map.of(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class));
        Map<Class<?>, KafkaOperations<?, ?>> templates = new LinkedHashMap<>();
        templates.put(byte[].class, bytesTemplate);
        templates.put(Object.class, kafkaTemplate);
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                templates, (record, exception) -> new TopicPartition(record.topic() + DLT_SUFFIX, record.partition()));
        return new DefaultErrorHandler(recoverer, new FixedBackOff(1000L, 3L));
    }
}
