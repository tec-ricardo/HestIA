package br.com.hestia.microtreinamento.service;

import br.com.hestia.microtreinamento.model.TipoErro;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class FonteRecorrenciaConformidadeTest {

    private JdbcTemplate jdbc;
    private FonteRecorrenciaConformidade fonte;

    private final LocalDateTime conclusao =
            LocalDateTime.of(2025, 1, 15, 12, 0);

    @BeforeEach
    void preparar() {
        var dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:" + UUID.randomUUID()
                        + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
                "sa",
                ""
        );

        jdbc = new JdbcTemplate(dataSource);
        fonte = new FonteRecorrenciaConformidade(jdbc);

        jdbc.execute("""
                CREATE TABLE usuarios (
                    id BIGINT PRIMARY KEY,
                    empresa_id BIGINT NOT NULL
                )
                """);

        jdbc.execute("""
                CREATE TABLE registros_uso_ia (
                    id BIGINT PRIMARY KEY,
                    usuario_id BIGINT NOT NULL,
                    data_hora TIMESTAMP NOT NULL
                )
                """);

        jdbc.execute("""
                CREATE TABLE politica_uso_versoes (
                    id BIGINT PRIMARY KEY,
                    politica_uso_id BIGINT NOT NULL,
                    empresa_id BIGINT NOT NULL
                )
                """);

        jdbc.execute("""
                CREATE TABLE avaliacoes_conformidade (
                    id BIGINT PRIMARY KEY,
                    registro_uso_id BIGINT UNIQUE,
                    politica_uso_id BIGINT,
                    politica_versao_id BIGINT,
                    data_avaliacao TIMESTAMP,
                    status VARCHAR(30),
                    regra_codigo VARCHAR(60),
                    regra_versao VARCHAR(20)
                )
                """);

        jdbc.execute("""
                CREATE TABLE avaliacao_criterios_atendidos (
                    avaliacao_id BIGINT,
                    criterio VARCHAR(40)
                )
                """);

        jdbc.update(
                "INSERT INTO usuarios VALUES (10, 1), (20, 2)"
        );

        jdbc.update(
                "INSERT INTO politica_uso_versoes VALUES (100, 50, 1)"
        );
    }

    @AfterEach
    void encerrar() {
        jdbc.execute("SHUTDOWN");
    }

    @Test
    void deveLerOcorrenciasEAferirCoberturaNoBanco() {
        registrar(1, 10, conclusao.minusDays(1));
        avaliar(1, "NAO_CONFORME", false);

        registrar(2, 10, conclusao.plusDays(1));
        avaliar(2, "CONFORME", true);

        var leitura = consultar();

        assertTrue(leitura.coberturaConfirmada());
        assertEquals(1, leitura.antes().registros());
        assertEquals(1, leitura.depois().avaliados());
        assertEquals(1, leitura.ocorrencias().size());
        assertEquals(
                "AVALIACAO:1:FERRAMENTA_NAO_AUTORIZADA",
                leitura.ocorrencias().getFirst().origemId()
        );
    }

    @Test
    void registroSemAvaliacaoDeveImpedirCoberturaCompleta() {
        registrar(1, 10, conclusao.minusDays(1));
        avaliar(1, "NAO_CONFORME", false);
        registrar(2, 10, conclusao.plusDays(1));

        var leitura = consultar();

        assertFalse(leitura.coberturaConfirmada());
        assertEquals(1, leitura.depois().registros());
        assertEquals(0, leitura.depois().avaliados());
    }

    @Test
    void deveExcluirOutraEmpresaEOsLimitesExternos() {
        registrar(1, 20, conclusao.minusDays(1));
        avaliar(1, "NAO_CONFORME", false);

        registrar(2, 10, conclusao.minusDays(30).minusSeconds(1));
        avaliar(2, "NAO_CONFORME", false);

        registrar(3, 10, conclusao.plusDays(30));
        avaliar(3, "NAO_CONFORME", false);

        var leitura = consultar();

        assertTrue(leitura.ocorrencias().isEmpty());
        assertEquals(0, leitura.antes().registros());
        assertEquals(0, leitura.depois().registros());
        assertFalse(leitura.coberturaConfirmada());
    }

    @Test
    void pendenciaDeRevisaoNaoDeveVirarErroConfirmado() {
        registrar(1, 10, conclusao.minusDays(1));
        avaliar(1, "REQUER_REVISAO", false);

        var leitura = consultar();

        assertTrue(leitura.ocorrencias().isEmpty());
        assertEquals(0, leitura.antes().avaliados());
    }

    @Test
    void regraDesconhecidaEAvaliacaoFuturaNaoDevemSerUtilizadas() {
        registrar(1, 10, conclusao.minusDays(1));
        avaliar(1, "NAO_CONFORME", false);

        jdbc.update("""
                UPDATE avaliacoes_conformidade
                SET regra_versao = 'desconhecida'
                WHERE id = 1
                """);

        registrar(2, 10, conclusao.plusDays(1));
        avaliar(2, "NAO_CONFORME", false);

        jdbc.update("""
                UPDATE avaliacoes_conformidade
                SET data_avaliacao = ?
                WHERE id = 2
                """, Timestamp.valueOf(conclusao.plusDays(31)));

        var leitura = consultar();

        assertTrue(leitura.ocorrencias().isEmpty());
        assertFalse(leitura.coberturaConfirmada());
    }

    @Test
    void tipoSemConfirmacaoIndividualDevePermanecerIndisponivel() {
        var leitura = fonte.consultar(
                1L,
                10L,
                TipoErro.DADOS_SENSIVEIS,
                conclusao,
                conclusao.plusDays(30)
        );

        assertFalse(leitura.tipoSuportado());
        assertFalse(leitura.coberturaConfirmada());
        assertTrue(leitura.ocorrencias().isEmpty());
    }

    private FonteRecorrenciaConformidade.Leitura consultar() {
        return fonte.consultar(
                1L,
                10L,
                TipoErro.FERRAMENTA_NAO_AUTORIZADA,
                conclusao,
                conclusao.plusDays(30)
        );
    }

    private void registrar(
            long id,
            long usuarioId,
            LocalDateTime data
    ) {
        jdbc.update(
                "INSERT INTO registros_uso_ia VALUES (?, ?, ?)",
                id,
                usuarioId,
                Timestamp.valueOf(data)
        );
    }

    private void avaliar(
            long registroId,
            String status,
            boolean criterioAtendido
    ) {
        jdbc.update("""
                INSERT INTO avaliacoes_conformidade (
                    id,
                    registro_uso_id,
                    politica_uso_id,
                    politica_versao_id,
                    data_avaliacao,
                    status,
                    regra_codigo,
                    regra_versao
                )
                SELECT id, id, 50, 100, data_hora, ?,
                       'CONFORMIDADE_CRITERIOS', '1.0'
                FROM registros_uso_ia
                WHERE id = ?
                """, status, registroId);

        if (criterioAtendido) {
            jdbc.update("""
                    INSERT INTO avaliacao_criterios_atendidos
                    VALUES (?, 'IA_AUTORIZADA')
                    """, registroId);
        }
    }
}