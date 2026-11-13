package br.com.hestia.indicadores.maturidade;

import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import java.time.LocalDate;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;

class AdaptadorOperacionalMaturidadeTest {
    JdbcTemplate jdbc;AdaptadorOperacionalMaturidade service;
    @BeforeEach void preparar(){
        jdbc=new JdbcTemplate(new DriverManagerDataSource("jdbc:h2:mem:"+UUID.randomUUID()+";DB_CLOSE_DELAY=-1","sa",""));
        jdbc.execute("CREATE TABLE usuarios(id BIGINT, empresa_id BIGINT, departamento_id BIGINT, ativo BOOLEAN)");
        jdbc.execute("CREATE TABLE aprendizagem_ciclos(id BIGINT, usuario_id BIGINT, empresa_id BIGINT, catalogo_id BIGINT)");
        jdbc.execute("CREATE TABLE aprendizagem_tentativas(id BIGINT, ciclo_id BIGINT, nota NUMERIC(5,2), status VARCHAR, finalizada_em TIMESTAMP)");
        jdbc.execute("CREATE TABLE aprendizagem_catalogo(id BIGINT, versao INT, empresa_id BIGINT, treinamento_id BIGINT, conteudo_sha256 VARCHAR(64), tema VARCHAR, publicado_em TIMESTAMP)");
        jdbc.execute("CREATE TABLE catalogo_treinamentos(id BIGINT, empresa_id BIGINT, conteudo_sha256 VARCHAR(64), tema VARCHAR, estado VARCHAR, publicado_em TIMESTAMP, retirado_em TIMESTAMP)");
        jdbc.execute("CREATE TABLE aprendizagem_leituras(empresa_id BIGINT, usuario_id BIGINT, treinamento_id BIGINT, conteudo_sha256 VARCHAR(64), conclusao_id BIGINT, concluida_em TIMESTAMP)");
        jdbc.execute("CREATE TABLE conclusoes_microtreinamento(id BIGINT, usuario_id BIGINT, microtreinamento_id BIGINT, concluido BOOLEAN, data_conclusao TIMESTAMP)");
        jdbc.execute("CREATE TABLE registros_uso_ia(id BIGINT, usuario_id BIGINT, data_hora TIMESTAMP, ferramenta_id BIGINT)");
        jdbc.execute("CREATE TABLE autorizacoes_ferramentas_ia(empresa_id BIGINT, ferramenta_id BIGINT, status VARCHAR, inicio_vigencia TIMESTAMP, fim_vigencia TIMESTAMP)");
        jdbc.execute("CREATE TABLE politica_uso_versoes(id BIGINT, empresa_id BIGINT, politica_uso_id BIGINT)");
        jdbc.execute("CREATE TABLE avaliacoes_conformidade(id BIGINT, registro_uso_id BIGINT, politica_uso_id BIGINT, politica_versao_id BIGINT, regra_codigo VARCHAR, regra_versao VARCHAR, motivo_preservado VARCHAR, regra_preservada VARCHAR, limitacoes_preservadas VARCHAR, status VARCHAR, data_avaliacao TIMESTAMP)");
        jdbc.update("INSERT INTO usuarios VALUES(1,1,10,TRUE),(2,2,20,TRUE)");jdbc.update("INSERT INTO politica_uso_versoes VALUES(1,1,1),(2,2,2)");
        service=new AdaptadorOperacionalMaturidade(jdbc,new CalculadorasMaturidade(),new FonteEvidenciasSeguranca());
    }
    void inserir(int id,int empresa,String status,boolean fundamentos){
        jdbc.update("INSERT INTO registros_uso_ia(id, usuario_id, data_hora) VALUES(?,?,TIMESTAMP '2026-01-01 12:00:00')",id,empresa);
        jdbc.update("INSERT INTO avaliacoes_conformidade VALUES(?,?,?,?,?,?,?,?,?,?,TIMESTAMP '2026-01-02 12:00:00')",id,id,empresa,fundamentos?empresa:null,"R","1","motivo","regra","limites",status);
    }
    @Test void usaFundamentosPreservadosEMantemLegadasSeparadas(){
        for(int i=1;i<=8;i++)inserir(i,1,"CONFORME",true);
        inserir(9,1,"NAO_CONFORME",true);inserir(10,1,"NAO_CONFORME",true);
        for(int i=11;i<=15;i++)inserir(i,1,"REQUER_REVISAO",true);
        inserir(16,1,"CONFORME",false);inserir(17,2,"CONFORME",true);
        var f=service.calcular(1L,null,LocalDate.of(2026,1,1),LocalDate.of(2026,1,31)).stream().filter(x->x.codigo().equals("usoResponsavel")).findFirst().orElseThrow();
        assertThat(f.nota()).isEqualByComparingTo("80");assertThat(f.estatisticas().get("avaliacoesLegadas")).isEqualByComparingTo("1");assertThat(f.elegivelGlobal()).isFalse();
    }
    @Test void notaECoberturaUsamMesmoPeriodoComLimitesInclusivoEExclusivo(){
        inserir(1,1,"CONFORME",true);
        inserir(2,1,"NAO_CONFORME",true);
        inserir(3,1,"NAO_CONFORME",true);
        inserir(4,1,"NAO_CONFORME",true);
        inserir(5,1,"NAO_CONFORME",true);
        inserir(6,1,"CONFORME",false);
        jdbc.update("UPDATE registros_uso_ia SET data_hora=TIMESTAMP '2025-12-31 23:59:59' WHERE id IN (2,6)");
        jdbc.update("UPDATE registros_uso_ia SET data_hora=TIMESTAMP '2026-02-01 00:00:00' WHERE id=3");
        jdbc.update("UPDATE avaliacoes_conformidade SET data_avaliacao=TIMESTAMP '2026-02-01 00:00:00' WHERE id=4");
        jdbc.update("UPDATE registros_uso_ia SET data_hora=TIMESTAMP '2026-01-01 00:00:00' WHERE id=5");
        jdbc.update("UPDATE avaliacoes_conformidade SET data_avaliacao=TIMESTAMP '2026-01-31 23:59:59' WHERE id=5");
        var f=service.calcular(1L,10L,LocalDate.of(2026,1,1),LocalDate.of(2026,1,31)).stream().filter(x->x.codigo().equals("usoResponsavel")).findFirst().orElseThrow();
        assertThat(f.nota()).isEqualByComparingTo("50");
        assertThat(f.amostra()).isEqualTo(2);
        assertThat(f.populacao()).isEqualTo(3);
        assertThat(f.estatisticas().get("avaliacoesLegadas")).isEqualByComparingTo("0");
        assertThat(f.estatisticas().get("coberturaAvaliacao")).isEqualByComparingTo(CalculadorasMaturidade.razao(2,3));
    }
    @Test void periodoSemAvaliacoesNaoGeraZero(){
        inserir(1,1,"CONFORME",true);
        var f=service.calcular(1L,10L,LocalDate.of(2026,10,1),LocalDate.of(2026,10,21)).stream().filter(x->x.codigo().equals("usoResponsavel")).findFirst().orElseThrow();
        assertThat(f.nota()).isNull();assertThat(f.estado()).isEqualTo("SEM_DADOS");
    }
    @Test
    void classificaPelaVigenciaNaDataDoUso() {
        jdbc.update("""
                INSERT INTO autorizacoes_ferramentas_ia VALUES
                (1, 7, 'APROVADA', TIMESTAMP '2026-01-02 00:00:00', TIMESTAMP '2026-01-03 00:00:00'),
                (1, 7, 'DESCONTINUADA', TIMESTAMP '2026-01-03 00:00:00', NULL),
                (2, 7, 'APROVADA', TIMESTAMP '2026-01-01 00:00:00', NULL)
                """);
        jdbc.update("""
                INSERT INTO registros_uso_ia VALUES
                (1, 1, TIMESTAMP '2026-01-01 00:00:00', 7),
                (2, 1, TIMESTAMP '2026-01-02 00:00:00', 7),
                (3, 1, TIMESTAMP '2026-01-03 00:00:00', 7)
                """);
        var fator = service.calcular(1L, 10L, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 3))
                .stream().filter(item -> item.codigo().equals("governanca")).findFirst().orElseThrow();
        assertThat(fator.nota()).isEqualByComparingTo("50");
        assertThat(fator.amostra()).isEqualTo(2L);
        assertThat(fator.populacao()).isEqualTo(3L);
        assertThat(fator.coberturaClassificacao()).isEqualByComparingTo(CalculadorasMaturidade.razao(2, 3));
    }


    @Test void calculaLiteracyComUltimaTentativaValidaPorPessoa() {
        jdbc.update("INSERT INTO usuarios VALUES (3,1,10,TRUE)");
        jdbc.update("INSERT INTO catalogo_treinamentos VALUES (100,1,'hash','tema','PUBLICADO',TIMESTAMP '2025-12-01 00:00:00',NULL)");
        jdbc.update("INSERT INTO aprendizagem_catalogo VALUES (100,1,1,100,'hash','tema',TIMESTAMP '2025-12-01 00:00:00')");
        jdbc.update("INSERT INTO aprendizagem_ciclos VALUES (1,1,1,100),(2,3,1,100)");
        jdbc.update("""
                INSERT INTO aprendizagem_tentativas VALUES
                (1,1,40,'REPROVADA',TIMESTAMP '2026-01-01 10:00:00'),
                (2,1,60,'REPROVADA',TIMESTAMP '2026-01-02 10:00:00'),
                (3,2,100,'APROVADA',TIMESTAMP '2026-01-01 10:00:00')
                """);
        var fator = service.calcular(1L,10L,LocalDate.of(2026,1,1),LocalDate.of(2026,1,31))
                .stream().filter(item -> item.codigo().equals("aiLiteracy")).findFirst().orElseThrow();
        assertThat(fator.nota()).isEqualByComparingTo("80");
        assertThat(fator.amostra()).isEqualTo(2L);
    }
}
