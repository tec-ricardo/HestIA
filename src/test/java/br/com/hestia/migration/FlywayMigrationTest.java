package br.com.hestia.migration;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FlywayMigrationTest {
    @Test
    void deveAplicarTodasAsMigracoesEmSequencia() {
        Flyway flyway = Flyway.configure()
                .dataSource("jdbc:h2:mem:flyway;MODE=PostgreSQL;DB_CLOSE_DELAY=-1", "sa", "")
                .locations("classpath:db/migration")
                .load();

        flyway.migrate();

        var aplicadas = flyway.info().applied();
        assertEquals(8, aplicadas.length);
        assertEquals("8", aplicadas[aplicadas.length - 1].getVersion().getVersion());
    }
}
