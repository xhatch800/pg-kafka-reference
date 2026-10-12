package com.example.sample.db;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import liquibase.Liquibase;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

/**
 * Runs docker/postgres/init-db.sh the same way compose does (via /docker-entrypoint-initdb.d) and checks the
 * result: a non-superuser owner of the application database that can run the Liquibase migrations.
 */
@Testcontainers
class PostgresInitScriptIT {

    @Container
    static final PostgreSQLContainer postgres = new PostgreSQLContainer(DockerImageName.parse("postgres:17"))
            .withEnv("APP_DB", "sample")
            .withEnv("APP_USER", "dev")
            .withEnv("APP_PASSWORD", "welcome1")
            .withCopyFileToContainer(
                    MountableFile.forHostPath("docker/postgres/init-db.sh", 0755),
                    "/docker-entrypoint-initdb.d/init-db.sh");

    @Test
    void createsNonSuperuserOwnerThatCanRunMigrations() throws Exception {
        String url = "jdbc:postgresql://" + postgres.getHost() + ":"
                + postgres.getMappedPort(PostgreSQLContainer.POSTGRESQL_PORT) + "/sample";

        try (Connection connection = DriverManager.getConnection(url, "dev", "welcome1")) {
            assertThat(queryString(connection, "select current_user")).isEqualTo("dev");
            assertThat(queryString(connection, "select rolsuper::text from pg_roles where rolname = current_user"))
                    .isEqualTo("false");
            assertThat(queryString(connection,
                    "select pg_get_userbyid(datdba) from pg_database where datname = current_database()"))
                    .isEqualTo("dev");

            new Liquibase("db/changelog/db.changelog-master.xml", new ClassLoaderResourceAccessor(),
                    new JdbcConnection(connection)).update();

            assertThat(queryString(connection, "select count(*)::text from databasechangelog")).isEqualTo("3");
            assertThat(queryString(connection, "select tableowner from pg_tables where tablename = 'employees'"))
                    .isEqualTo("dev");
        }
    }

    private static String queryString(Connection connection, String sql) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet resultSet = statement.executeQuery()) {
            assertThat(resultSet.next()).as("row for: %s", sql).isTrue();
            return resultSet.getString(1);
        }
    }
}
