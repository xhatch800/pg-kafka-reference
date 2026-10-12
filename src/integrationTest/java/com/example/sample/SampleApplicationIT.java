package com.example.sample;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class SampleApplicationIT {

    @Autowired
    EntityManager entityManager;

    @Test
    void contextLoadsAndLiquibaseAppliesAllChangesets() {
        @SuppressWarnings("unchecked")
        List<String> ids = entityManager
                .createNativeQuery("select id from databasechangelog order by orderexecuted")
                .getResultList();

        assertThat(ids).containsExactly(
                "001-create-departments-and-employees",
                "002-add-employee-termination",
                "003-case-insensitive-department-name");
    }
}
