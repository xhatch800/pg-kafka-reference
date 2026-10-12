package com.example.sample.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.kafka.clients.admin.NewTopic;
import org.junit.jupiter.api.Test;

class KafkaConfigTest {

    private final KafkaConfig config = new KafkaConfig();

    @Test
    void mainTopicHasConfiguredNameAndSinglePartition() {
        NewTopic topic = config.employeeTerminationsTopic("employee-terminations");

        assertThat(topic.name()).isEqualTo("employee-terminations");
        assertThat(topic.numPartitions()).isEqualTo(1);
        assertThat(topic.replicationFactor()).isEqualTo((short) 1);
    }

    @Test
    void deadLetterTopicUsesDltSuffix() {
        NewTopic topic = config.employeeTerminationsDltTopic("employee-terminations");

        assertThat(topic.name()).isEqualTo("employee-terminations.DLT");
        assertThat(topic.numPartitions()).isEqualTo(1);
    }
}
