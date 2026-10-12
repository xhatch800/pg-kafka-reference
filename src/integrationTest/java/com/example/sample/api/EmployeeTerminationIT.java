package com.example.sample.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.awaitility.Awaitility.await;

import com.example.sample.config.KafkaConfig;
import com.example.sample.dto.request.TerminateEmployeeRequest;
import com.example.sample.dto.response.DepartmentResponse;
import com.example.sample.dto.response.EmployeeResponse;
import com.example.sample.messaging.EmployeeTerminationEvent;
import com.example.sample.messaging.EmployeeTerminationPublisher;
import com.example.sample.support.AbstractApiIT;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.testcontainers.kafka.KafkaContainer;

class EmployeeTerminationIT extends AbstractApiIT {

    private static final Duration TIMEOUT = Duration.ofSeconds(30);

    @Autowired
    KafkaContainer kafka;

    @Autowired
    EmployeeTerminationPublisher publisher;

    @Value("${app.kafka.topics.employee-terminations}")
    String topic;

    @Test
    void terminationIsAcceptedThenAppliedAsynchronously() {
        EmployeeResponse employee = newEmployee();

        ResponseEntity<Void> response = requestTermination(employee.id(), "Restructuring", LocalDate.of(2026, 10, 31));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        await().atMost(TIMEOUT).untilAsserted(() -> {
            EmployeeResponse current = getEmployee(employee.id());
            assertThat(current.status()).isEqualTo("TERMINATED");
            assertThat(current.terminatedAt()).isEqualTo(LocalDate.of(2026, 10, 31));
            assertThat(current.terminationReason()).isEqualTo("Restructuring");
        });
    }

    @Test
    void secondTerminationAfterProcessingIsRejected() {
        EmployeeResponse employee = newEmployee();
        requestTermination(employee.id(), "First", null);
        await().atMost(TIMEOUT).until(() -> "TERMINATED".equals(getEmployee(employee.id()).status()));

        ResponseEntity<Void> second = requestTermination(employee.id(), "Second", null);

        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(getEmployee(employee.id()).terminationReason()).isEqualTo("First");
    }

    @Test
    void terminationOfUnknownEmployeeReturns404() {
        ResponseEntity<Void> response = requestTermination(987_654_321L, "Restructuring", null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void duplicateEventDeliveryIsIgnored() {
        EmployeeResponse employee = newEmployee();
        publisher.publish(new EmployeeTerminationEvent(employee.id(), "First", LocalDate.of(2026, 10, 1), Instant.now()));
        publisher.publish(new EmployeeTerminationEvent(employee.id(), "Second", LocalDate.of(2026, 10, 2), Instant.now()));

        await().atMost(TIMEOUT).until(() -> "TERMINATED".equals(getEmployee(employee.id()).status()));
        // Give the second event time to be consumed, then confirm it changed nothing.
        await().pollDelay(Duration.ofSeconds(2)).atMost(TIMEOUT).untilAsserted(() -> {
            EmployeeResponse current = getEmployee(employee.id());
            assertThat(current.terminationReason()).isEqualTo("First");
            assertThat(current.terminatedAt()).isEqualTo(LocalDate.of(2026, 10, 1));
        });
    }

    @Test
    void eventForMissingEmployeeGoesToDeadLetterTopic() {
        long missingId = 900_000_000L + ThreadLocalRandom.current().nextLong(1_000_000L);

        publisher.publish(new EmployeeTerminationEvent(missingId, "Ghost", LocalDate.of(2026, 10, 31), Instant.now()));

        ConsumerRecord<String, byte[]> dead = pollDeadLetter(String.valueOf(missingId));
        assertThat(new String(dead.value(), StandardCharsets.UTF_8)).contains("\"employeeId\":" + missingId);
    }

    @Test
    void poisonMessageGoesToDltAndConsumerKeepsWorking() throws Exception {
        String key = "poison-" + UUID.randomUUID();
        try (KafkaProducer<String, String> producer = new KafkaProducer<>(Map.<String, Object>of(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers(),
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class))) {
            producer.send(new ProducerRecord<>(topic, key, "this is not json")).get();
        }

        ConsumerRecord<String, byte[]> dead = pollDeadLetter(key);
        assertThat(new String(dead.value(), StandardCharsets.UTF_8)).isEqualTo("this is not json");

        EmployeeResponse employee = newEmployee();
        requestTermination(employee.id(), "After poison", null);
        await().atMost(TIMEOUT).until(() -> "TERMINATED".equals(getEmployee(employee.id()).status()));
    }

    private EmployeeResponse newEmployee() {
        DepartmentResponse department = createDepartment(unique("Dept"));
        return createEmployee(department.id(), unique("emp") + "@example.com");
    }

    private ResponseEntity<Void> requestTermination(Long id, String reason, LocalDate effectiveDate) {
        return client.post().uri("/api/employees/{id}/termination", id)
                .body(new TerminateEmployeeRequest(reason, effectiveDate))
                .retrieve()
                .toBodilessEntity();
    }

    private ConsumerRecord<String, byte[]> pollDeadLetter(String key) {
        Map<String, Object> props = Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers(),
                ConsumerConfig.GROUP_ID_CONFIG, "dlt-reader-" + UUID.randomUUID(),
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest",
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class);
        try (KafkaConsumer<String, byte[]> consumer = new KafkaConsumer<>(props)) {
            consumer.subscribe(List.of(topic + KafkaConfig.DLT_SUFFIX));
            long deadline = System.nanoTime() + TIMEOUT.toNanos();
            while (System.nanoTime() < deadline) {
                for (ConsumerRecord<String, byte[]> record : consumer.poll(Duration.ofMillis(500))) {
                    if (key.equals(record.key())) {
                        return record;
                    }
                }
            }
        }
        return fail("No dead-letter record with key " + key + " within " + TIMEOUT);
    }
}
