package br.com.hestia.indicadores.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CoberturaMaturidadeServiceTest {

    private final CoberturaMaturidadeService service =
            new CoberturaMaturidadeService();

    private final ComposicaoMaturidadeService composicao =
            new ComposicaoMaturidadeService();

    @Test
    void semNotasDeveRetornarCoberturaZeroEScoreAusente() {
        var resultado = service.calcular(
                composicao.consultar(),
                Map.of()
        );

        assertEquals(8, resultado.totalFatores());
        assertEquals(0, resultado.fatoresDisponiveis());
        assertEquals(new BigDecimal("0.00"), resultado.coberturaPercentual());
        assertNull(resultado.score());

        for (var fator : resultado.fatores()) {
            assertFalse(fator.disponivel());
            assertNull(fator.nota());
            assertNotNull(fator.motivoAusencia());
        }
    }

    @Test
    void notaZeroDeveContarComoDisponivel() {
        var resultado = service.calcular(
                composicao.consultar(),
                Map.of("seguranca", BigDecimal.ZERO)
        );

        assertEquals(1, resultado.fatoresDisponiveis());
        assertEquals(new BigDecimal("12.50"), resultado.coberturaPercentual());
        assertNull(resultado.score());

        var seguranca = resultado.fatores().stream()
                .filter(fator -> fator.codigo().equals("seguranca"))
                .findFirst()
                .orElseThrow();

        assertTrue(seguranca.disponivel());
        assertEquals(BigDecimal.ZERO, seguranca.nota());
        assertNull(seguranca.motivoAusencia());
    }

    @Test
    void coberturaParcialNaoDeveGerarScoreNormalizado() {
        var resultado = service.calcular(
                composicao.consultar(),
                Map.of(
                        "seguranca", new BigDecimal("100"),
                        "governanca", new BigDecimal("100")
                )
        );

        assertEquals(2, resultado.fatoresDisponiveis());
        assertEquals(new BigDecimal("25.00"), resultado.coberturaPercentual());
        assertNull(resultado.score());
    }

    @Test
    void notasCompletasDevemGerarScorePonderado() {
        var resultado = service.calcular(
                composicao.consultar(),
                Map.of(
                        "aiLiteracy", new BigDecimal("72"),
                        "governanca", new BigDecimal("65"),
                        "usoResponsavel", new BigDecimal("80"),
                        "produtividade", new BigDecimal("76"),
                        "finOps", new BigDecimal("58"),
                        "seguranca", new BigDecimal("85"),
                        "colaboracao", new BigDecimal("70"),
                        "desenvolvimentoCompetencias", new BigDecimal("68")
                )
        );

        assertEquals(8, resultado.fatoresDisponiveis());
        assertEquals(new BigDecimal("100.00"), resultado.coberturaPercentual());
        assertEquals(new BigDecimal("73.25"), resultado.score());
    }

    @Test
    void notaNulaDevePermanecerAusente() {
        var notas = new HashMap<String, BigDecimal>();
        notas.put("seguranca", null);

        var resultado = service.calcular(composicao.consultar(), notas);

        assertEquals(0, resultado.fatoresDisponiveis());
        assertNull(resultado.score());
    }

    @Test
    void devePreservarPeriodoEVersaoDaComposicao() {
        var regra = composicao.consultar(
                LocalDate.of(2026, 10, 1),
                LocalDate.of(2026, 10, 31),
                "1.0"
        );

        var resultado = service.calcular(regra, Map.of());

        assertEquals("1.0", resultado.versaoRegra());
        assertEquals(regra.periodoReferencia(), resultado.periodoReferencia());
    }

    @Test
    void deveRejeitarNotasForaDaEscala() {
        for (String valor : new String[]{"-1", "100.01"}) {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> service.calcular(
                            composicao.consultar(),
                            Map.of("seguranca", new BigDecimal(valor))
                    )
            );
        }
    }

    @Test
    void deveRejeitarFatorDesconhecido() {
        assertThrows(
                IllegalArgumentException.class,
                () -> service.calcular(
                        composicao.consultar(),
                        Map.of("fatorInexistente", BigDecimal.TEN)
                )
        );
    }
}