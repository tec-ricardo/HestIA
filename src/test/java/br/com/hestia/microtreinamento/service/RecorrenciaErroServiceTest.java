package br.com.hestia.microtreinamento.service;

import br.com.hestia.microtreinamento.dto.OcorrenciaErroDTO;
import br.com.hestia.microtreinamento.model.TipoErro;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RecorrenciaErroServiceTest {

    private final RecorrenciaErroService service =
            new RecorrenciaErroService();

    private final LocalDateTime conclusao =
            LocalDateTime.of(2026, 9, 1, 12, 0);

    @Test
    void deveCompararOcorrenciasAntesEDepoisSemDuplicar() {
        var primeira = ocorrencia("a", conclusao.minusDays(3));

        var resultado = calcular(
                conclusao.plusDays(30),
                List.of(
                        primeira,
                        primeira,
                        ocorrencia("b", conclusao.minusDays(2)),
                        ocorrencia("c", conclusao.minusDays(1)),
                        ocorrencia("d", conclusao.plusDays(1))
                )
        );

        assertEquals(3, resultado.ocorrenciasAntes());
        assertEquals(1, resultado.ocorrenciasDepois());
        assertTrue(resultado.recorrenteAntes());
        assertFalse(resultado.recorrenteDepois());
        assertTrue(resultado.periodoPosteriorCompleto());
    }

    @Test
    void deveRespeitarLimitesSemContarConclusaoDuasVezes() {
        var resultado = calcular(
                conclusao.plusDays(30),
                List.of(
                        ocorrencia("a", conclusao.minusDays(30)),
                        ocorrencia("b", conclusao.minusDays(30).minusNanos(1)),
                        ocorrencia("c", conclusao),
                        ocorrencia("d", conclusao.plusDays(30).minusNanos(1)),
                        ocorrencia("e", conclusao.plusDays(30))
                )
        );

        assertEquals(1, resultado.ocorrenciasAntes());
        assertEquals(2, resultado.ocorrenciasDepois());
    }

    @Test
    void deveIgnorarOutraEmpresaUsuarioOuTipoDeErro() {
        var resultado = calcular(
                conclusao.plusDays(30),
                List.of(
                        new OcorrenciaErroDTO(
                                "a", 2L, 10L,
                                TipoErro.DADOS_SENSIVEIS,
                                conclusao.plusDays(1)
                        ),
                        new OcorrenciaErroDTO(
                                "b", 1L, 20L,
                                TipoErro.DADOS_SENSIVEIS,
                                conclusao.plusDays(1)
                        ),
                        new OcorrenciaErroDTO(
                                "c", 1L, 10L,
                                TipoErro.VIOLACAO_LGPD,
                                conclusao.plusDays(1)
                        )
                )
        );

        assertEquals(0, resultado.ocorrenciasAntes());
        assertEquals(0, resultado.ocorrenciasDepois());
    }

    @Test
    void deveSinalizarPeriodoIncompletoEExcluirEventosFuturos() {
        var resultado = calcular(
                conclusao.plusDays(5),
                List.of(
                        ocorrencia("a", conclusao.plusDays(2)),
                        ocorrencia("b", conclusao.plusDays(6))
                )
        );

        assertEquals(1, resultado.ocorrenciasDepois());
        assertFalse(resultado.periodoPosteriorCompleto());
    }

    @Test
    void deveRejeitarMesmoIdentificadorComDatasDiferentes() {
        assertThrows(
                IllegalArgumentException.class,
                () -> calcular(
                        conclusao.plusDays(30),
                        List.of(
                                ocorrencia("a", conclusao.minusDays(1)),
                                ocorrencia("a", conclusao.plusDays(1))
                        )
                )
        );
    }

    @Test
    void deveRejeitarObservacaoAnteriorAConclusao() {
        assertThrows(
                IllegalArgumentException.class,
                () -> calcular(conclusao.minusDays(1), List.of())
        );
    }

    @Test
    void deveAceitarAusenciaDeOcorrenciasSemMarcarRecorrencia() {
        var resultado = calcular(
                conclusao.plusDays(30),
                List.of()
        );

        assertEquals(0, resultado.ocorrenciasAntes());
        assertEquals(0, resultado.ocorrenciasDepois());
        assertFalse(resultado.recorrenteAntes());
        assertFalse(resultado.recorrenteDepois());
    }

    private RecorrenciaErroService.Resultado calcular(
            LocalDateTime observadoAte,
            List<OcorrenciaErroDTO> ocorrencias
    ) {
        return service.calcular(
                1L,
                10L,
                TipoErro.DADOS_SENSIVEIS,
                conclusao,
                observadoAte,
                ocorrencias
        );
    }

    private OcorrenciaErroDTO ocorrencia(
            String origemId,
            LocalDateTime data
    ) {
        return new OcorrenciaErroDTO(
                origemId,
                1L,
                10L,
                TipoErro.DADOS_SENSIVEIS,
                data
        );
    }
}