package br.com.hestia.rag.service;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;

@EnabledIfEnvironmentVariable(named="HESTIA_SPRINT_PG_TEST_URL", matches=".+")
class ConversasRagPostgresTest {
    private JdbcTemplate jdbc;
    private DriverManagerDataSource ds;
    private final String schema="conversas_"+UUID.randomUUID().toString().replace("-","");
    @BeforeEach void preparar() {
        ds=new DriverManagerDataSource(System.getenv("HESTIA_SPRINT_PG_TEST_URL"),
            System.getenv("HESTIA_PG_TEST_USER"),System.getenv("HESTIA_PG_TEST_PASSWORD"));
        jdbc=new JdbcTemplate(ds);
        assertThat(jdbc.queryForObject("SELECT current_database()",String.class)).isEqualTo("hestia_validacao");
        jdbc.execute("CREATE SCHEMA "+schema);
        ds.setUrl(ds.getUrl()+(ds.getUrl().contains("?")?"&":"?")+"currentSchema="+schema+",public");
        jdbc=new JdbcTemplate(ds);
        flyway("23").migrate();
    }
    private Flyway flyway(String target) {
        return Flyway.configure().dataSource(ds).schemas(schema).defaultSchema(schema)
            .locations("classpath:db/migration").target(target).load();
    }
    @AfterEach void limpar() {
        if(jdbc!=null && ds!=null && ds.getUrl().contains("currentSchema="+schema))
            jdbc.execute("DROP SCHEMA IF EXISTS "+schema+" CASCADE");
    }
    private void autores() {
        jdbc.update("INSERT INTO empresas(id,nome,cnpj) VALUES(1,'Teste','11111111111111'),(2,'Outra','22222222222222')");
        jdbc.update("INSERT INTO departamentos(id,nome,empresa_id) VALUES(1,'Teste',1),(2,'Outra',2)");
        jdbc.update("INSERT INTO usuarios(id,nome,email,senha,cargo,perfil,ativo,data_cadastro,empresa_id,departamento_id) VALUES(1,'Autor','autor@example.test','fixture','Teste','FUNCIONARIO',true,CURRENT_TIMESTAMP,1,1),(2,'Outro','outro@example.test','fixture','Teste','FUNCIONARIO',true,CURRENT_TIMESTAMP,1,1),(3,'Outra','outra@example.test','fixture','Teste','FUNCIONARIO',true,CURRENT_TIMESTAMP,2,2)");
    }
    @Test void migraBaseVaziaEPermitePrimeiraConversa() {
        flyway("24").migrate();
        autores();
        Long id=jdbc.queryForObject("INSERT INTO conversas_rag(empresa_id,usuario_id,titulo,criada_em) VALUES(1,1,'Nova',CURRENT_TIMESTAMP) RETURNING id",Long.class);
        assertThat(id).isEqualTo(1);
        flyway("24").validate();
        assertThat(flyway("24").migrate().migrationsExecuted).isZero();
    }
    @Test void preservaLegadoEImpedeVinculoEntreAutoresEEmpresas() {
        autores();
        jdbc.update("INSERT INTO interacoes_rag(id,empresa_id,usuario_id,chave,pergunta,modo,estado,resposta,fontes,criada_em) VALUES(42,1,1,'antiga','Pergunta preservada','CONSULTAR','RECUPERADA','Texto preservado','[]',CURRENT_TIMESTAMP),(73,2,3,'outra','Outra pergunta','CONSULTAR','RECUPERADA','Outro texto','[]',CURRENT_TIMESTAMP)");
        assertThat(flyway("24").migrate().migrationsExecuted).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT resposta FROM interacoes_rag WHERE id=42",String.class)).isEqualTo("Texto preservado");
        Long primeira=jdbc.queryForObject("SELECT conversa_id FROM interacoes_rag WHERE id=42",Long.class);
        Long outra=jdbc.queryForObject("SELECT conversa_id FROM interacoes_rag WHERE id=73",Long.class);
        assertThat(primeira).isNotEqualTo(outra);
        assertThat(jdbc.queryForObject("SELECT titulo FROM conversas_rag WHERE id=?",String.class,primeira)).isEqualTo("Pergunta preservada");
        assertThat(jdbc.queryForObject("SELECT usuario_id FROM conversas_rag WHERE id=?",Long.class,outra)).isEqualTo(3);
        Long nova=jdbc.queryForObject("INSERT INTO conversas_rag(empresa_id,usuario_id,titulo,criada_em) VALUES(1,1,'Nova',CURRENT_TIMESTAMP) RETURNING id",Long.class);
        assertThat(nova).isGreaterThan(Math.max(primeira,outra));
        assertThatThrownBy(()->jdbc.update("UPDATE interacoes_rag SET conversa_id=? WHERE id=42",outra))
            .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        assertThatThrownBy(()->jdbc.update("UPDATE interacoes_rag SET usuario_id=2 WHERE id=42"))
            .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM interacoes_rag",Long.class)).isEqualTo(2);
        flyway("24").validate();
        assertThat(flyway("24").migrate().migrationsExecuted).isZero();
    }
}
