package com.pashumandi.milk;

import org.flywaydb.core.Flyway;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlywayMigrationTest {

    @Test
    @DisplayName("Verify Flyway migration V1 executes successfully and builds valid schema")
    void testFlywayMigrationExecution() {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:flyway_test_db;MODE=MySQL;DB_CLOSE_DELAY=-1");
        dataSource.setUser("sa");
        dataSource.setPassword("");

        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .load();

        var migrationResult = flyway.migrate();
        assertTrue(migrationResult.success);
        assertEquals(1, migrationResult.migrationsExecuted);
    }
}
