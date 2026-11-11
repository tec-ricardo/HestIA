package br.com.hestia.indicadores.maturidade;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RankingDepartamentalControllerTest {
    @Test
    void preservaEmpateEDeixaSemPosicaoQuemNaoTemScoreGlobal() {
        var linhas = RankingDepartamentalController.ordenar(List.of(
                linha(4L, "Sem dados", null),
                linha(2L, "B", "80.00"),
                linha(3L, "C", "70"),
                linha(1L, "A", "80")
        ), true);

        assertEquals(List.of(1L, 2L, 3L, 4L),
                linhas.stream().map(RankingDepartamentalController.Linha::departamentoId).toList());
        assertEquals(Arrays.asList(1, 1, 3, null),
                linhas.stream().map(RankingDepartamentalController.Linha::posicao).toList());
    }

    @Test
    void gestorNaoRecebePosicaoBaseadaApenasNoProprioDepartamento() {
        var linhas = RankingDepartamentalController.ordenar(List.of(linha(1L, "A", "80")), false);
        assertNull(linhas.getFirst().posicao());
    }

    private RankingDepartamentalController.Linha linha(Long id, String nome, String score) {
        var resultado = mock(ResultadoMaturidadeService.Resultado.class);
        when(resultado.scoreGlobal()).thenReturn(score == null ? null : new BigDecimal(score));
        return new RankingDepartamentalController.Linha(id, nome, null, resultado);
    }
}
