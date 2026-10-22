package br.com.hestia.microtreinamento.service;

import br.com.hestia.microtreinamento.dto.OcorrenciaErroDTO;
import br.com.hestia.microtreinamento.model.TipoErro;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EfetividadeCapacitacaoServiceTest {

    private final EfetividadeCapacitacaoService service =
            new EfetividadeCapacitacaoService();

    private final LocalDateTime conclusao =
            LocalDateTime.of(2025, 1, 15, 12, 0);

    @Test
    void deveIdentificarMelhoria() {
        var resultado = service.avaliar(
                acompanhamento(4, 1, true),
                true
        );

        assertEquals(
                EfetividadeCapacitacaoService.Situacao.MELHORIA,
                resultado.situacao()
        );
        assertEquals(
                new BigDecimal("75.00"),
                resultado.reducaoPercentual()
        );
    }

    @Test
    void deveIdentificarEstabilidade() {
        var resultado = service.avaliar(
                acompanhamento(3, 3, true),
                true
        );

        assertEquals(
                EfetividadeCapacitacaoService.Situacao.ESTABILIDADE,
                resultado.situacao()
        );
        assertEquals(
                new BigDecimal("0.00"),
                resultado.reducaoPercentual()
        );
    }

    @Test
    void deveIdentificarPioraComReducaoNegativa() {
        var resultado = service.avaliar(
                acompanhamento(2, 3, true),
                true
        );

        assertEquals(
                EfetividadeCapacitacaoService.Situacao.PIORA,
                resultado.situacao()
        );
        assertEquals(
                new BigDecimal("-50.00"),
                resultado.reducaoPercentual()
        );
    }

    @Test
    void deveAceitarZeroPosteriorQuandoCoberturaEstaConfirmada() {
        var resultado = service.avaliar(
                acompanhamento(3, 0, true),
                true
        );

        assertEquals(
                EfetividadeCapacitacaoService.Situacao.MELHORIA,
                resultado.situacao()
        );
        assertEquals(
                new BigDecimal("100.00"),
                resultado.reducaoPercentual()
        );
    }

    @Test
    void naoDeveAfirmarMelhoriaSemCoberturaConfirmada() {
        var resultado = service.avaliar(
                acompanhamento(3, 0, true),
                false
        );

        assertInsuficiente(
                resultado,
                EfetividadeCapacitacaoService.Motivo
                        .COBERTURA_NAO_CONFIRMADA
        );
    }

    @Test
    void naoDeveCompararPeriodoPosteriorIncompleto() {
        var resultado = service.avaliar(
                acompanhamento(3, 1, false),
                true
        );

        assertInsuficiente(
                resultado,
                EfetividadeCapacitacaoService.Motivo
                        .PERIODO_POSTERIOR_INCOMPLETO
        );
    }

    @Test
    void naoDeveDividirPorZeroNemClassificarSemBaseAnterior() {
        for (int depois : List.of(0, 2)) {
            var resultado = service.avaliar(
                    acompanhamento(0, depois, true),
                    true
            );

            assertInsuficiente(
                    resultado,
                    EfetividadeCapacitacaoService.Motivo
                            .SEM_OCORRENCIAS_ANTERIORES
            );
        }
    }

    @Test
    void deveArredondarPercentualEPreservarVinculo() {
        var acompanhamento = acompanhamento(3, 2, true);

        var resultado = service.avaliar(acompanhamento, true);

        assertEquals(
                new BigDecimal("33.33"),
                resultado.reducaoPercentual()
        );
        assertSame(acompanhamento, resultado.acompanhamento());
        assertEquals(
                100L,
                resultado.acompanhamento().conclusaoId()
        );
    }

    @Test
    void deveRejeitarAcompanhamentoAusente() {
        assertThrows(
                IllegalArgumentException.class,
                () -> service.avaliar(null, true)
        );
    }

    private void assertInsuficiente(
            EfetividadeCapacitacaoService.Resultado resultado,
            EfetividadeCapacitacaoService.Motivo motivo
    ) {
        assertEquals(
                EfetividadeCapacitacaoService.Situacao
                        .DADOS_INSUFICIENTES,
                resultado.situacao()
        );
        assertEquals(motivo, resultado.motivo());
        assertNull(resultado.reducaoPercentual());
    }

    private RecorrenciaCapacitacaoService.Resultado acompanhamento(
            int antes,
            int depois,
            boolean completo
    ) {
        var ocorrencias = new ArrayList<OcorrenciaErroDTO>();

        for (int i = 0; i < antes; i++) {
            ocorrencias.add(new OcorrenciaErroDTO(
                    "antes-" + i,
                    1L,
                    10L,
                    TipoErro.DADOS_SENSIVEIS,
                    conclusao.minusDays(1).plusSeconds(i)
            ));
        }

        for (int i = 0; i < depois; i++) {
            ocorrencias.add(new OcorrenciaErroDTO(
                    "depois-" + i,
                    1L,
                    10L,
                    TipoErro.DADOS_SENSIVEIS,
                    conclusao.plusDays(1).plusSeconds(i)
            ));
        }

        var calculo = new RecorrenciaErroService().calcular(
                1L,
                10L,
                TipoErro.DADOS_SENSIVEIS,
                conclusao,
                conclusao.plusDays(completo ? 30 : 5),
                ocorrencias
        );

        return new RecorrenciaCapacitacaoService.Resultado(
                100L,
                1L,
                "Proteção de Dados em IA",
                calculo
        );
    }
}