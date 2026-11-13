package br.com.hestia.indicadores.maturidade;

import br.com.hestia.indicadores.maturidade.ApuracaoColaboracao.PullRequest;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ApuracaoColaboracaoTest {

    private static final LocalDate INICIO = LocalDate.of(2026, 10, 1);
    private static final LocalDate FIM = LocalDate.of(2026, 10, 31);

    private final ApuracaoColaboracao apuracao =
            new ApuracaoColaboracao(new CalculadorasMaturidade());

    @Test
    void calculaColaboracaoComPrsUnicosNoPeriodoEDepartamento() {
        var revisado = new PullRequest("repo-1", 1, INICIO, 1L, 10L, 100L, 2);
        var semRevisao = new PullRequest("repo-2", 1, FIM, 1L, 10L, 100L, 0);
        var semAutor = new PullRequest("repo-1", 2, FIM, 1L, 10L, null, 1);
        var foraDoPeriodo = new PullRequest("repo-1", 3, FIM.plusDays(1), 1L, 10L, 100L, 1);
        var outroDepartamento = new PullRequest("repo-1", 4, FIM, 1L, 20L, 101L, 1);
        var outraEmpresa = new PullRequest("repo-3", 1, FIM, 2L, 10L, 200L, 1);

        var resultado = apuracao.calcular(1L, 10L, INICIO, FIM, List.of(
                revisado, revisado, semRevisao, semAutor,
                foraDoPeriodo, outroDepartamento, outraEmpresa
        ));

        assertThat(resultado.populacao()).isEqualTo(3L);
        assertThat(resultado.amostra()).isEqualTo(2L);
        assertThat(resultado.numerador()).isEqualByComparingTo("1");
        assertThat(resultado.denominador()).isEqualByComparingTo("2");
        assertThat(resultado.nota()).isEqualByComparingTo("50");
    }

    @Test
    void excluiPrComRegistrosDivergentesDaAmostraValida() {
        var primeiro = new PullRequest("repo-1", 1, INICIO, 1L, 10L, 100L, 0);
        var divergente = new PullRequest("repo-1", 1, INICIO, 1L, 10L, 100L, 2);

        var resultado = apuracao.calcular(
                1L, 10L, INICIO, FIM, List.of(primeiro, divergente)
        );

        assertThat(resultado.populacao()).isEqualTo(1L);
        assertThat(resultado.amostra()).isZero();
        assertThat(resultado.nota()).isNull();
    }
}