package br.com.hestia.indicadores.maturidade;

import br.com.hestia.usuario.model.Usuario;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class InvestigacaoMaturidadeControllerTest {

    @Test
    void retornaSomentePendenciasDoEscopoEPeriodo() {
        var jdbc = new JdbcTemplate(new DriverManagerDataSource(
                "jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1", "sa", ""));
        jdbc.execute("CREATE TABLE registros_uso_ia (id BIGINT, data_hora TIMESTAMP)");
        jdbc.execute("CREATE TABLE avaliacoes_conformidade (id BIGINT, registro_uso_id BIGINT, data_avaliacao TIMESTAMP)");
        jdbc.execute("""
                CREATE TABLE tratamentos_nao_conformidade (
                    id BIGINT, avaliacao_id BIGINT, empresa_id BIGINT, departamento_id BIGINT,
                    status VARCHAR(30), responsavel_id BIGINT
                )
                """);
        jdbc.update("""
                INSERT INTO registros_uso_ia VALUES
                (1, TIMESTAMP '2026-10-01 00:00:00'),
                (2, TIMESTAMP '2026-09-30 23:59:59')
                """);
        jdbc.update("""
                INSERT INTO avaliacoes_conformidade VALUES
                (11, 1, TIMESTAMP '2026-10-31 23:59:59'),
                (12, 1, TIMESTAMP '2026-11-01 00:00:00'),
                (13, 2, TIMESTAMP '2026-10-01 10:00:00')
                """);
        jdbc.update("""
                INSERT INTO tratamentos_nao_conformidade VALUES
                (21, 11, 1, 10, 'EM_TRATAMENTO', 42),
                (22, 11, 1, 10, 'ABERTA', NULL),
                (23, 11, 1, 10, 'RESOLVIDA', 42),
                (24, 11, 2, 10, 'ABERTA', NULL),
                (25, 11, 1, 20, 'ABERTA', NULL),
                (26, 12, 1, 10, 'ABERTA', NULL),
                (27, 13, 1, 10, 'ABERTA', NULL)
                """);

        var inicio = LocalDate.of(2026, 10, 1);
        var fim = LocalDate.of(2026, 10, 31);
        var usuario = mock(Usuario.class);
        var resultados = mock(ResultadoMaturidadeService.class);
        var maturidade = mock(ResultadoMaturidadeService.Resultado.class);
        when(maturidade.empresaId()).thenReturn(1L);
        when(maturidade.departamentoId()).thenReturn(10L);
        when(resultados.consultar(usuario, new ResultadoMaturidadeService.Consulta(
                inicio, fim, ResultadoMaturidadeService.Natureza.OPERACIONAL, null, 10L, null, null)))
                .thenReturn(maturidade);

        when(maturidade.fatores()).thenReturn(java.util.List.of(
                new CalculadorasMaturidade().governancaOperacional(30, 30, 24)));

        var consulta = new InvestigacaoMaturidadeController(
                resultados, jdbc, new ClassificacaoRiscoDepartamental())
                .consultar(usuario, inicio, fim, 10L);

        assertThat(consulta.maturidade()).isSameAs(maturidade);
        assertThat(consulta.risco().estado()).isEqualTo("SEM_CLASSIFICACAO");
        assertThat(consulta.risco().evidencias())
                .extracting(ClassificacaoRiscoDepartamental.Evidencia::fator)
                .containsExactly("governanca");
        assertThat(consulta.risco().pendencias())
                .contains("usoResponsavel: sem nota válida", "seguranca: sem nota válida");
        assertThat(consulta.pendencias()).containsExactly(
                new InvestigacaoMaturidadeController.Pendencia(21L, 11L, "EM_TRATAMENTO", 42L),
                new InvestigacaoMaturidadeController.Pendencia(22L, 11L, "ABERTA", null));
    }

    @Test
    void retornaRiscoClassificadoComEvidenciasDosTresFatores() {
        var jdbc = mock(JdbcTemplate.class);
        var resultados = mock(ResultadoMaturidadeService.class);
        var maturidade = mock(ResultadoMaturidadeService.Resultado.class);
        var usuario = mock(Usuario.class);
        var inicio = LocalDate.of(2026, 10, 1);
        var fim = inicio.plusDays(30);
        when(resultados.consultar(usuario, new ResultadoMaturidadeService.Consulta(
                inicio, fim, ResultadoMaturidadeService.Natureza.OPERACIONAL, null, 10L, null, null)))
                .thenReturn(maturidade);
        when(maturidade.empresaId()).thenReturn(1L);
        when(maturidade.departamentoId()).thenReturn(10L);

        var calculo = new CalculadorasMaturidade();
        var totalControles = CatalogoControlesSeguranca.Controle.values().length;
        when(maturidade.fatores()).thenReturn(java.util.List.of(
                calculo.governancaOperacional(30, 30, 24),
                calculo.usoResponsavel(24, 6, 0, 0, 30, 30),
                fatorSeguranca(totalControles)));

        var risco = new InvestigacaoMaturidadeController(
                resultados, jdbc, new ClassificacaoRiscoDepartamental())
                .consultar(usuario, inicio, fim, 10L).risco();

        assertThat(risco.estado()).isEqualTo("CLASSIFICADO");
        assertThat(risco.indice()).isEqualByComparingTo("12");
        assertThat(risco.classificacao()).isEqualTo(ClassificacaoRiscoDepartamental.Faixa.BAIXO);
        assertThat(risco.pendencias()).isEmpty();
        assertThat(risco.evidencias())
                .extracting(ClassificacaoRiscoDepartamental.Evidencia::fator)
                .containsExactly("governanca", "usoResponsavel", "seguranca");
        assertThat(risco.evidencias()).allMatch(ClassificacaoRiscoDepartamental.Evidencia::suficiente);
    }

    private FatorCalculado fatorSeguranca(int totalControles) {
        var total = java.math.BigDecimal.valueOf(totalControles);
        return new FatorCalculado("seguranca", new java.math.BigDecimal("0.20"),
                new java.math.BigDecimal("100"), "CALCULADO", true, "Controles verificados",
                java.math.BigDecimal.ONE, java.math.BigDecimal.ONE, totalControles, (long) totalControles,
                total, total, "Controles atendidos / controles avaliados", "Evidências de teste",
                java.util.Map.of(), java.util.List.of(), CalculadorasMaturidade.VERSAO);
    }

}
