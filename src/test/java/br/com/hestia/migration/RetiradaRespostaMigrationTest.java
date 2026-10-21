package br.com.hestia.migration;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class RetiradaRespostaMigrationTest {
    @Test void devePreservarRetiradasLegadasSemInventarJustificativaAutorOuData() throws Exception {
        String url = "jdbc:h2:mem:retirada_" + UUID.randomUUID() + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1";
        var ds = new SingleConnectionDataSource(url, "sa", "", true);
        try {
            var anterior = Flyway.configure().dataSource(ds).locations("classpath:db/migration").target("17").load();
            anterior.migrate();
            try (var c=ds.getConnection(); var s=c.createStatement()) {
                s.executeUpdate("""
                    INSERT INTO respostas_reutilizaveis(id,empresa_id,prompt_original,resposta_gerada,reutilizavel)
                    VALUES(900,1,'Pergunta legada','Conteúdo legado',FALSE)
                    """);
            }
            var atual=Flyway.configure().dataSource(ds).locations("classpath:db/migration").load();
            atual.migrate();
            try(var c=ds.getConnection();var s=c.createStatement();
                    var rs=s.executeQuery("SELECT * FROM respostas_reutilizaveis WHERE id=900")) {
                assertTrue(rs.next()); assertFalse(rs.getBoolean("reutilizavel"));
                assertEquals("Conteúdo legado",rs.getString("resposta_gerada"));
                assertNull(rs.getObject("retirada_por_id")); assertNull(rs.getTimestamp("retirada_em"));
                assertNull(rs.getString("justificativa_retirada"));
            }
            assertEquals(0,atual.migrate().migrationsExecuted);
        } finally { ds.destroy(); }
    }
}
