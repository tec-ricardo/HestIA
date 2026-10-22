package br.com.hestia.indicadores.maturidade;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import java.sql.Connection;
import java.time.LocalDate;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

/** Executar somente em banco descartável hestia_validacao, nunca na base do projeto. */
@EnabledIfEnvironmentVariable(named="HESTIA_MATURIDADE_PG_TEST_URL", matches=".+")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MaturidadePostgresTest {
    DriverManagerDataSource ds; Connection connection; JdbcTemplate jdbc;
    Flyway flyway; int atualizacoes;
    final String schema="maturidade_"+UUID.randomUUID().toString().replace("-","");
    @BeforeAll void migrar() {
        ds=new DriverManagerDataSource(System.getenv("HESTIA_MATURIDADE_PG_TEST_URL"),
            System.getenv("HESTIA_PG_TEST_USER"),System.getenv("HESTIA_PG_TEST_PASSWORD"));
        assertThat(new JdbcTemplate(ds).queryForObject("SELECT current_database()",String.class))
            .as("Proteção: requer banco descartável exclusivo").isEqualTo("hestia_validacao");
        new JdbcTemplate(ds).execute("CREATE SCHEMA "+schema);
        ds.setUrl(ds.getUrl()+(ds.getUrl().contains("?")?"&":"?")+"currentSchema="+schema+",public");
        Flyway.configure().dataSource(ds).schemas(schema).defaultSchema(schema).locations("classpath:db/migration").target("17").load().migrate();
        flyway=Flyway.configure().dataSource(ds).schemas(schema).defaultSchema(schema).locations("classpath:db/migration").load();
        atualizacoes=flyway.migrate().migrationsExecuted;
    }
    @AfterAll void removerSchemaDeTeste() {
        if(ds!=null)new JdbcTemplate(ds).execute("DROP SCHEMA IF EXISTS "+schema+" CASCADE");
    }
    @BeforeEach void transacao() throws Exception {
        connection=ds.getConnection(); connection.setAutoCommit(false);
        jdbc=new JdbcTemplate(new SingleConnectionDataSource(connection,true));
    }
    @AfterEach void desfazer() throws Exception {
        if(connection!=null){try {connection.rollback();} finally {connection.close();}}
    }
    @Test void atualizaBase17Para23EValidaHistorico() {
        assertThat(atualizacoes).isEqualTo(6);
        flyway.validate();
        assertThat(flyway.info().applied()).hasSize(23);
        assertThat(flyway.migrate().migrationsExecuted).isZero();
        assertThat(jdbc.queryForList("SELECT tablename FROM pg_tables WHERE schemaname=?",String.class,schema))
            .contains("trilhas","trilha_etapas","participacoes_trilha","trilha_conclusoes");
    }
    void linha(String hash,String tabela,String chave,String json) {
        jdbc.update("INSERT INTO base_sintetica.import_linhas(origem_sha256,tabela,chave_origem,dados) VALUES(?,?,?,CAST(? AS jsonb))",hash,tabela,chave,json);
    }
    @Test void jsonbIsolaEmpresaSnapshotEProjetos() {
        String hash="a".repeat(64), outro="b".repeat(64);
        linha(hash,"employees","E1","{\"company_id\":\"C01\",\"employee_id\":\"E1\"}");
        linha(hash,"employees","E2","{\"company_id\":\"C02\",\"employee_id\":\"E2\"}");
        linha(outro,"employees","E3","{\"company_id\":\"C01\",\"employee_id\":\"E3\"}");
        linha(hash,"projects","P1","{\"company_id\":\"C01\",\"project_id\":\"P1\"}");
        linha(hash,"projects","P2","{\"company_id\":\"C02\",\"project_id\":\"P2\"}");
        linha(hash,"issues","I1","{\"project_id\":\"P1\",\"issue_id\":\"I1\"}");
        linha(hash,"issues","I2","{\"project_id\":\"P2\",\"issue_id\":\"I2\"}");
        linha(outro,"issues","I3","{\"project_id\":\"P1\",\"issue_id\":\"I3\"}");
        linha(hash,"ai_usage_logs","U1","{\"company_id\":\"C01\",\"ai_tool_id\":\"T1\"}");
        linha(hash,"ai_usage_logs","U2","{\"company_id\":\"C02\",\"ai_tool_id\":\"T2\"}");
        linha(hash,"ai_tools","T1","{\"ai_tool_id\":\"T1\"}");
        linha(hash,"ai_tools","T2","{\"ai_tool_id\":\"T2\"}");
        var repo=new SnapshotMaturidadeRepository(jdbc);
        assertThat(repo.indice("employees","employee_id",hash,"C01")).containsOnlyKeys("E1");
        assertThat(repo.indice("projects","project_id",hash,"C01")).containsOnlyKeys("P1");
        assertThat(repo.indice("issues","issue_id",hash,"C01")).containsOnlyKeys("I1");
        assertThat(repo.indice("ai_tools","ai_tool_id",hash,"C01")).containsOnlyKeys("T1");
    }
    @Test void projetoAmbiguoEntreEmpresasBloqueiaConsulta() {
        String hash="c".repeat(64);
        linha(hash,"projects","1","{\"company_id\":\"C01\",\"project_id\":\"P1\"}");
        linha(hash,"projects","2","{\"company_id\":\"C02\",\"project_id\":\"P1\"}");
        assertThatThrownBy(()->new SnapshotMaturidadeRepository(jdbc).indice("projects","project_id",hash,"C01"))
            .isInstanceOf(IllegalStateException.class).hasMessageContaining("ambíguo");
    }
    @Test void conformidadeUsaMesmoPeriodoNoPostgresReal() {
        jdbc.update("INSERT INTO empresas(id,nome,cnpj) VALUES(1,'Teste','11111111111111'),(2,'Outra','22222222222222')");
        jdbc.update("INSERT INTO departamentos(id,nome,empresa_id) VALUES(10,'Teste',1),(20,'Outra',2)");
        jdbc.update("INSERT INTO usuarios(id,nome,email,senha,cargo,perfil,ativo,data_cadastro,empresa_id,departamento_id) VALUES(1,'Teste','teste@example.test','fixture','Teste','ADMIN',true,CURRENT_TIMESTAMP,1,10),(2,'Outra','outra@example.test','fixture','Teste','ADMIN',true,CURRENT_TIMESTAMP,2,20)");
        jdbc.update("INSERT INTO politicas_uso(id,titulo,descricao,conteudo,versao,ativa,data_criacao,data_atualizacao,empresa_id) SELECT id,'Teste','Teste','Teste','1',true,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,id FROM empresas");
        jdbc.update("INSERT INTO politica_uso_versoes(id,politica_uso_id,empresa_id,revisao,titulo,descricao,conteudo,versao,ativa,excluida,origem,registrada_em) SELECT id,id,id,1,'Teste','Teste','Teste','1',true,false,'TESTE',CURRENT_TIMESTAMP FROM empresas");
        jdbc.update("INSERT INTO ferramentas_ia(id,nome,fornecedor,tipo,finalidade_uso,status,nivel_risco,trata_dados_pessoais,data_cadastro,data_atualizacao,empresa_id) SELECT id,'Teste','Teste','TESTE','Teste','ATIVA','BAIXO',false,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,id FROM empresas");
        jdbc.update("INSERT INTO registros_uso_ia(id,usuario_id,ferramenta_id,modelo_ia,data_hora,finalidade,status_execucao) VALUES(1,1,1,'Teste','2026-01-01','TESTE','SUCESSO'),(2,1,1,'Teste','2025-12-31','TESTE','SUCESSO'),(3,1,1,'Teste','2026-01-31 23:59:59','TESTE','SUCESSO'),(4,1,1,'Teste','2026-01-10','TESTE','SUCESSO'),(5,2,2,'Teste','2026-01-10','TESTE','SUCESSO')");
        jdbc.update("INSERT INTO avaliacoes_conformidade(registro_uso_id,politica_uso_id,politica_versao_id,status,data_avaliacao,regra_codigo,regra_versao,motivo_preservado,regra_preservada,limitacoes_preservadas) VALUES(1,1,1,'CONFORME','2026-01-02','R','1','M','R','L'),(2,1,1,'NAO_CONFORME','2026-01-02','R','1','M','R','L'),(3,1,1,'NAO_CONFORME','2026-01-31 23:59:59','R','1','M','R','L'),(4,1,1,'NAO_CONFORME','2026-02-01','R','1','M','R','L'),(5,2,2,'NAO_CONFORME','2026-01-02','R','1','M','R','L')");
        var fator=new AdaptadorOperacionalMaturidade(jdbc,new CalculadorasMaturidade())
            .calcular(1L,10L,LocalDate.of(2026,1,1),LocalDate.of(2026,1,31)).stream()
            .filter(f->f.codigo().equals("usoResponsavel")).findFirst().orElseThrow();
        assertThat(fator.nota()).isEqualByComparingTo("50");
        assertThat(fator.populacao()).isEqualTo(3);
        assertThat(fator.amostra()).isEqualTo(2);
        assertThat(fator.estatisticas().get("coberturaAvaliacao")).isEqualByComparingTo(CalculadorasMaturidade.razao(2,3));
    }
}
