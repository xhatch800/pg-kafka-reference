package com.example.sample.messaging;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

@ExtendWith(MockitoExtension.class)
class EmployeeTerminationPublisherTest {

    private static final EmployeeTerminationEvent EVENT = new EmployeeTerminationEvent(
            7L, "Restructuring", LocalDate.of(2026, 10, 31), Instant.parse("2026-10-11T10:00:00Z"));

    @Mock
    KafkaTemplate<String, EmployeeTerminationEvent> kafkaTemplate;

    EmployeeTerminationPublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new EmployeeTerminationPublisher(kafkaTemplate, "employee-terminations");
    }

    @Test
    void publishSendsToTopicKeyedByEmployeeId() {
        when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(CompletableFuture.completedFuture(null));

        publisher.publish(EVENT);

        verify(kafkaTemplate).send("employee-terminations", "7", EVENT);
    }

    @Test
    void publishPropagatesBrokerFailure() {
        when(kafkaTemplate.send(anyString(), anyString(), any()))
                .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("broker down")));

        assertThatThrownBy(() -> publisher.publish(EVENT))
                .isInstanceOf(CompletionException.class)
                .hasRootCauseMessage("broker down");
    }
}
