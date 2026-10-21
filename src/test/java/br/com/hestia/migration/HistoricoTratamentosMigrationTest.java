package br.com.hestia.migration;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class HistoricoTratamentosMigrationTest {
    @Test void deveMigrarSomenteEstadoConhecidoSemInventarAutoriaOuEventosAnteriores() throws Exception {
        String url = "jdbc:h2:mem:tratamentos_migracao_" + UUID.randomUUID()
                + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1";
        // Mantém a conexão de criação dos CHECKs ativa durante a prova de upgrade no H2.
        var fonte = new org.springframework.jdbc.datasource.SingleConnectionDataSource(url, "sa", "", true);
        var anterior = Flyway.configure().dataSource(fonte).locations("classpath:db/migration").target("17").load();
        anterior.migrate();
        try (var c = anterior.getConfiguration().getDataSource().getConnection(); var s = c.createStatement()) {
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
                    VALUES(990,990,990,'NAO_CONFORME')
                    """);
            s.executeUpdate("""
                    INSERT INTO tratamentos_nao_conformidade(id,avaliacao_id,empresa_id,departamento_id,
                        criado_por_id,criado_em,status,responsavel_id,acao_corretiva,versao_registro)
                    VALUES(990,990,990,990,990,'2026-01-01 12:00:00','EM_TRATAMENTO',990,'Ação já existente',4)
                    """);
        }
        var atual = Flyway.configure().dataSource(fonte).locations("classpath:db/migration").target("18").load();
        assertEquals(1, atual.migrate().migrationsExecuted);
        try (var c = atual.getConfiguration().getDataSource().getConnection(); var s = c.createStatement()) {
            try (var rs = s.executeQuery("SELECT * FROM historico_tratamentos WHERE tratamento_id=990")) {
                assertTrue(rs.next());
                assertEquals("ESTADO_MIGRADO", rs.getString("tipo"));
                assertNull(rs.getObject("autor_id"));
                assertEquals(4, rs.getLong("sequencia"));
                assertEquals("EM_TRATAMENTO", rs.getString("status_tratamento"));
                assertEquals("Ação já existente", rs.getString("acao_corretiva"));
                assertEquals("NAO_CONFORME", rs.getString("resultado_vigente"));
                assertTrue(rs.getString("justificativa").contains("anteriores"));
                assertNotNull(rs.getTimestamp("registrado_em"));
                assertFalse(rs.next());
            }
            try (var rs = s.executeQuery("SELECT * FROM tratamentos_nao_conformidade WHERE id=990")) {
                assertTrue(rs.next());
                assertEquals("NAO_CONFORME", rs.getString("resultado_vigente"));
                assertEquals(0, rs.getLong("revisoes_avaliacao"));
                assertEquals(4, rs.getLong("versao_registro"));
                assertEquals(java.time.LocalDateTime.of(2026,1,1,12,0), rs.getTimestamp("criado_em").toLocalDateTime());
            }
            try (var rs = s.executeQuery("SELECT status, politica_versao_id FROM avaliacoes_conformidade WHERE id=990")) {
                assertTrue(rs.next());
                assertEquals("NAO_CONFORME", rs.getString("status"));
                assertNull(rs.getObject("politica_versao_id"));
            }
        }
        assertEquals(0, atual.migrate().migrationsExecuted);
        fonte.destroy();
    }
}
