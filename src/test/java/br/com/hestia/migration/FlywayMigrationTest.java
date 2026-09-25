package br.com.hestia.migration;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlywayMigrationTest {
    @Test
    void deveAplicarTodasAsMigracoesEmSequencia() throws SQLException {
        Flyway flyway = Flyway.configure()
                .dataSource("jdbc:h2:mem:flyway;MODE=PostgreSQL;DB_CLOSE_DELAY=-1", "sa", "")
                .locations("classpath:db/migration")
                .load();

        flyway.migrate();

        var aplicadas = flyway.info().applied();
        assertEquals(10, aplicadas.length);
        assertEquals("10", aplicadas[aplicadas.length - 1].getVersion().getVersion());

        try (var conexao = flyway.getConfiguration().getDataSource().getConnection();
             var tabelas = conexao.getMetaData().getTables(null, null, "%", new String[]{"TABLE"})) {
            var encontradas = new java.util.HashSet<String>();
            while (tabelas.next()) {
                encontradas.add(tabelas.getString("TABLE_NAME").toLowerCase());
            }
            assertTrue(encontradas.containsAll(Set.of(
                    "conclusoes_microtreinamento",
                    "historicos_microtreinamento",
                    "necessidades_microtreinamento")));
        }
    }
}
