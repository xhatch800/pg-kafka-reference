package com.example.sample.db;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import liquibase.Liquibase;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/** Proves every changeset's rollback works: apply all, roll back one at a time, then re-apply. */
@Testcontainers
class LiquibaseRollbackIT {

    private static final String CHANGELOG = "db/changelog/db.changelog-master.xml";

    @Container
    static final PostgreSQLContainer postgres = new PostgreSQLContainer(DockerImageName.parse("postgres:17"));

    @Test
    void everyChangesetRollsBackCleanlyAndCanBeReapplied() throws Exception {
        try (Connection connection = DriverManager.getConnection(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())) {
            Liquibase liquibase = new Liquibase(
                    CHANGELOG, new ClassLoaderResourceAccessor(), new JdbcConnection(connection));

            liquibase.update();
            assertThat(appliedChangesets(connection)).hasSize(3);
            assertThat(exists(connection, "select 1 from pg_indexes where indexname = 'uk_departments_name_lower'")).isTrue();

            liquibase.rollback(1, (String) null);
            assertThat(exists(connection, "select 1 from pg_indexes where indexname = 'uk_departments_name_lower'")).isFalse();
            assertThat(exists(connection, "select 1 from pg_constraint where conname = 'uk_departments_name'")).isTrue();

            liquibase.rollback(1, (String) null);
            assertThat(exists(connection, "select 1 from information_schema.columns where table_name = 'employees' and column_name = 'status'")).isFalse();
            assertThat(exists(connection, "select 1 from pg_constraint where conname = 'ck_employees_status'")).isFalse();

            liquibase.rollback(1, (String) null);
            assertThat(exists(connection, "select 1 from information_schema.tables where table_name = 'employees'")).isFalse();
            assertThat(exists(connection, "select 1 from information_schema.tables where table_name = 'departments'")).isFalse();
            assertThat(appliedChangesets(connection)).isEmpty();

            liquibase.update();
            assertThat(appliedChangesets(connection)).containsExactly(
                    "001-create-departments-and-employees",
                    "002-add-employee-termination",
                    "003-case-insensitive-department-name");
        }
    }

    private static boolean exists(Connection connection, String sql) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet resultSet = statement.executeQuery()) {
            return resultSet.next();
        }
    }

    private static List<String> appliedChangesets(Connection connection) throws SQLException {
        List<String> ids = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(
                        "select id from databasechangelog order by orderexecuted");
                ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                ids.add(resultSet.getString(1));
            }
        }
        return ids;
    }
}
