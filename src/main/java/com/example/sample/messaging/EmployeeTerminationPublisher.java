package com.example.sample.messaging;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class EmployeeTerminationPublisher {

    private final KafkaTemplate<String, EmployeeTerminationEvent> kafkaTemplate;
    private final String topic;

    public EmployeeTerminationPublisher(
            KafkaTemplate<String, EmployeeTerminationEvent> kafkaTemplate,
            @Value("${app.kafka.topics.employee-terminations}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    /** Sends the event keyed by employee id and waits for the broker to acknowledge it. */
    public void publish(EmployeeTerminationEvent event) {
        kafkaTemplate.send(topic, String.valueOf(event.employeeId()), event).join();
    }
}
