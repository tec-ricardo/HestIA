package br.com.hestia.indicadores.maturidade;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static br.com.hestia.indicadores.maturidade.CatalogoControlesSeguranca.*;
import static org.junit.jupiter.api.Assertions.*;

class FonteEvidenciasSegurancaTest {
    private static final LocalDate INICIO = LocalDate.of(2025, 1, 1);
    private static final LocalDate FIM = LocalDate.of(2025, 1, 31);
    private static final Instant DATA = Instant.parse("2025-01-15T12:00:00Z");

    @Test
    void exigeOitoControlesConclusivosParaElegibilidade() {
        var fonte = fonte();
        var registros = Arrays.stream(Controle.values()).map(c -> registro(1L, null, "abc", "qa",
                c, c == Controle.SEG08_RESTAURACAO ? Situacao.NAO_ATENDIDO : Situacao.ATENDIDO, DATA)).toList();
        fonte.setEvidencias(registros.subList(0, 7));
        assertFalse(fonte.calcular(1L, null, INICIO, FIM).elegivelGlobal());
        fonte.setEvidencias(registros);
        var resultado = fonte.calcular(1L, null, INICIO, FIM);
        assertTrue(resultado.elegivelGlobal());
        assertEquals(87.5, resultado.nota().doubleValue());
    }

    @Test
    void excluiOutroEscopoReleaseAmbienteEPeriodo() {
        var fonte = fonte();
        var c = Controle.SEG01_ACESSO_POR_PERFIL;
        var s = Situacao.ATENDIDO;
        fonte.setEvidencias(List.of(
                registro(2L, null, "abc", "qa", c, s, DATA),
                registro(1L, 2L, "abc", "qa", c, s, DATA),
                registro(1L, null, "antiga", "qa", c, s, DATA),
                registro(1L, null, "abc", "prod", c, s, DATA),
                registro(1L, null, "abc", "qa", c, s, Instant.parse("2025-02-01T00:00:00Z"))
        ));
        assertNull(fonte.calcular(1L, null, INICIO, FIM).nota());
        assertEquals(1L, fonte.calcular(1L, 2L, INICIO, FIM).amostra());
    }

    @Test
    void reavaliacaoSemVerificacaoSubstituiResultadoAnterior() {
        var fonte = fonte();
        var c = Controle.SEG01_ACESSO_POR_PERFIL;
        fonte.setEvidencias(List.of(
                registro(1L, null, "abc", "qa", c, Situacao.ATENDIDO, DATA),
                registro(1L, null, "abc", "qa", c, Situacao.NAO_VERIFICADO, DATA.plusSeconds(1))
        ));
        assertNull(fonte.calcular(1L, null, INICIO, FIM).nota());
    }

    private FonteEvidenciasSeguranca fonte() {
        var fonte = new FonteEvidenciasSeguranca();
        fonte.setRelease("abc");
        fonte.setAmbiente("qa");
        return fonte;
    }

    private FonteEvidenciasSeguranca.Registro registro(Long empresa, Long departamento,
                                                       String release, String ambiente, Controle controle, Situacao situacao, Instant data) {
        return new FonteEvidenciasSeguranca.Registro(empresa, departamento, release, ambiente,
                VERSAO, controle, situacao, "Relatório de teste", "Revisor", data);
    }
}
