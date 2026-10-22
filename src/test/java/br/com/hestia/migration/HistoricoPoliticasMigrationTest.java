package br.com.hestia.migration;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class HistoricoPoliticasMigrationTest {
    @Test
    void devePreservarEstadoConhecidoSemAtribuirVersaoAtualAAvaliacaoLegada() throws Exception {
        String url = "jdbc:h2:mem:politicas_" + UUID.randomUUID()
                + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1";
        var anterior = Flyway.configure().dataSource(url, "sa", "")
                .locations("classpath:db/migration").target("15").load();
        anterior.migrate();
        try (var c = anterior.getConfiguration().getDataSource().getConnection();
             var s = c.createStatement()) {
            s.executeUpdate("INSERT INTO empresas(id,nome,cnpj) VALUES(990,'Teste','00000000000990')");
            s.executeUpdate("INSERT INTO departamentos(id,nome,empresa_id) VALUES(990,'Teste',990)");
            s.executeUpdate("""
                    INSERT INTO usuarios(id,nome,email,senha,cargo,perfil,ativo,data_cadastro,empresa_id,departamento_id)
                    VALUES(990,'Teste','legado@example.test','teste','Teste','ADMIN',TRUE,CURRENT_TIMESTAMP,990,990)
                    """);
            s.executeUpdate("""
                    INSERT INTO ferramentas_ia(id,nome,fornecedor,tipo,finalidade_uso,status,nivel_risco,
                        trata_dados_pessoais,data_cadastro,data_atualizacao,empresa_id)
                    VALUES(990,'Teste','Teste','CHATBOT','Teste','APROVADA','NAO_AVALIADO',
                        FALSE,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,990)
                    """);
            s.executeUpdate("""
                    INSERT INTO politicas_uso(id,titulo,descricao,conteudo,versao,ativa,
                        data_criacao,data_atualizacao,empresa_id)
                    VALUES(990,'Politica','Descricao','Estado conhecido','2.0',TRUE,
                        CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,990)
                    """);
            s.executeUpdate("""
                    INSERT INTO registros_uso_ia(id,usuario_id,ferramenta_id,modelo_ia,finalidade,status_execucao)
                    VALUES(990,990,990,'Teste','PESQUISA','SUCESSO')
                    """);
            s.executeUpdate("""
                    INSERT INTO avaliacoes_conformidade(id,registro_uso_id,politica_uso_id,status)
                    VALUES(990,990,990,'CONFORME')
                    """);
        }
        var atual = Flyway.configure().dataSource(url, "sa", "")
                .locations("classpath:db/migration").load();
        atual.migrate();
        try (var c = atual.getConfiguration().getDataSource().getConnection();
             var s = c.createStatement()) {
            try (var rs = s.executeQuery("SELECT * FROM politica_uso_versoes WHERE politica_uso_id=990")) {
                assertTrue(rs.next());
                assertEquals(1, rs.getInt("revisao"));
                assertEquals("2.0", rs.getString("versao"));
                assertEquals("Estado conhecido", rs.getString("conteudo"));
                assertEquals("MIGRACAO", rs.getString("origem"));
                assertNull(rs.getObject("autor_id"));
                assertFalse(rs.next());
            }
            try (var rs = s.executeQuery("SELECT * FROM avaliacoes_conformidade WHERE id=990")) {
                assertTrue(rs.next());
                assertEquals("CONFORME", rs.getString("status"));
                assertNull(rs.getObject("politica_versao_id"));
                assertNull(rs.getString("regra_versao"));
                assertNull(rs.getString("motivo_preservado"));
            }
            s.executeUpdate("UPDATE politicas_uso SET conteudo='Alterado' WHERE id=990");
            try (var rs = s.executeQuery("SELECT conteudo FROM politica_uso_versoes WHERE politica_uso_id=990")) {
                assertTrue(rs.next());
                assertEquals("Estado conhecido", rs.getString(1));
            }
        }
        // Reexecutar o Flyway nao duplica a fotografia importada.
        assertEquals(0, atual.migrate().migrationsExecuted);
    }
}
