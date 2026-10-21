package br.com.hestia.indicadores.service;

import br.com.hestia.indicadores.dto.ComposicaoMaturidadeDTO;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CenariosConhecidosMaturidadeTest {

    private final CoberturaMaturidadeService service =
            new CoberturaMaturidadeService();

    private final ComposicaoMaturidadeDTO regra =
            new ComposicaoMaturidadeService().consultar();

    @Test
    void deveValidarTodasAsCombinacoesDeDisponibilidade() {
        int total = regra.fatores().size();
        int combinacoes = 1 << total;

        assertEquals(8, total);

        for (int mascara = 0; mascara < combinacoes; mascara++) {
            Map<String, BigDecimal> notas = new HashMap<>();

            for (int i = 0; i < total; i++) {
                if ((mascara & (1 << i)) != 0) {
                    notas.put(
                            regra.fatores().get(i).codigo(),
                            new BigDecimal("80")
                    );
                }
            }

            var resultado = service.calcular(regra, notas);
            int disponiveis = Integer.bitCount(mascara);

            BigDecimal percentualEsperado =
                    BigDecimal.valueOf(disponiveis)
                            .multiply(BigDecimal.valueOf(100))
                            .divide(
                                    BigDecimal.valueOf(total),
                                    2,
                                    RoundingMode.HALF_UP
                            );

            String cenario = "Máscara de disponibilidade: " + mascara;

            assertEquals(
                    disponiveis,
                    resultado.fatoresDisponiveis(),
                    cenario
            );
            assertEquals(
                    percentualEsperado,
                    resultado.coberturaPercentual(),
                    cenario
            );

            if (disponiveis == total) {
                assertEquals(
                        new BigDecimal("80.00"),
                        resultado.score(),
                        cenario
                );
            } else {
                assertNull(resultado.score(), cenario);
            }

            for (var fator : resultado.fatores()) {
                boolean informado = notas.containsKey(fator.codigo());

                assertEquals(informado, fator.disponivel(), cenario);
                assertEquals(notas.get(fator.codigo()), fator.nota(), cenario);

                if (informado) {
                    assertNull(fator.motivoAusencia(), cenario);
                } else {
                    assertNotNull(fator.motivoAusencia(), cenario);
                }
            }
        }
    }

    @Test
    void todasAsNotasZeroDevemRepresentarCoberturaCompleta() {
        var resultado = service.calcular(
                regra,
                notasUniformes(BigDecimal.ZERO)
        );

        assertEquals(8, resultado.fatoresDisponiveis());
        assertEquals(new BigDecimal("100.00"), resultado.coberturaPercentual());
        assertEquals(new BigDecimal("0.00"), resultado.score());
    }

    @Test
    void todasAsNotasMaximasDevemProduzirScoreCem() {
        var resultado = service.calcular(
                regra,
                notasUniformes(new BigDecimal("100"))
        );

        assertEquals(new BigDecimal("100.00"), resultado.score());
        assertEquals(new BigDecimal("100.00"), resultado.coberturaPercentual());
    }

    @Test
    void deveArredondarSomenteOScoreFinal() {
        var resultado = service.calcular(
                regra,
                notasUniformes(new BigDecimal("33.335"))
        );

        assertEquals(new BigDecimal("33.34"), resultado.score());

        for (var fator : resultado.fatores()) {
            assertEquals(new BigDecimal("33.335"), fator.nota());
        }
    }

    @Test
    void deveAplicarPesosSemConfundirCoberturaComPontuacao() {
        Map<String, BigDecimal> notas = notasUniformes(BigDecimal.ZERO);
        notas.put("seguranca", new BigDecimal("100"));

        var resultado = service.calcular(regra, notas);

        assertEquals(new BigDecimal("100.00"), resultado.coberturaPercentual());
        assertEquals(new BigDecimal("20.00"), resultado.score());
    }

    @Test
    void resultadoNaoDeveSerAlteradoPorMudancaPosteriorNaEntrada() {
        Map<String, BigDecimal> notas = notasUniformes(new BigDecimal("80"));
        var resultado = service.calcular(regra, notas);

        notas.clear();

        assertEquals(8, resultado.fatoresDisponiveis());
        assertEquals(new BigDecimal("80.00"), resultado.score());
        assertEquals(8, resultado.fatores().size());

        assertThrows(
                UnsupportedOperationException.class,
                () -> resultado.fatores().clear()
        );
    }

    private Map<String, BigDecimal> notasUniformes(BigDecimal nota) {
        Map<String, BigDecimal> notas = new HashMap<>();

        for (var fator : regra.fatores()) {
            notas.put(fator.codigo(), nota);
        }

        return notas;
    }
}