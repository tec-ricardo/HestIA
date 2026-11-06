package br.com.hestia.indicadores.maturidade;

import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import static br.com.hestia.indicadores.maturidade.CatalogoControlesSeguranca.*;
import static org.assertj.core.api.Assertions.*;

class CatalogoControlesSegurancaTest {
    private final Instant corte = Instant.parse("2026-11-06T12:00:00Z");
    private Evidencia evidencia(Controle c, Situacao s, String referencia, String release, Instant data) {
        return new Evidencia(c, s, referencia, release, data);
    }
    @Test void catalogoSemEvidenciasNaoAprovaNenhumControle() {
        var r = avaliar(List.of(), "release-1", corte);
        assertThat(r.nota()).isNull();
        assertThat(r.elegivelGlobal()).isFalse();
        assertThat(r.populacao()).isEqualTo(8);
    }
    @Test void oitoControlesVerificadosPodemTerNotaBaixaComDadosCompletos() {
        var es = Arrays.stream(Controle.values()).map(c -> evidencia(c,
            c == Controle.SEG08_RESTAURACAO ? Situacao.NAO_ATENDIDO : Situacao.ATENDIDO,
            "evidencia-testada", "release-1", corte)).toList();
        var r = avaliar(es, "release-1", corte);
        assertThat(r.nota()).isEqualByComparingTo("87.5");
        assertThat(r.elegivelGlobal()).isTrue();
        assertThat(r.amostra()).isEqualTo(8);
    }
    @Test void naoAtribuiConfiancaCompletaAUmaUnicaEvidencia() {
        var r = avaliar(List.of(evidencia(Controle.SEG01_ACESSO_POR_PERFIL,
            Situacao.ATENDIDO, "teste", "release-1", corte)), "release-1", corte);
        assertThat(r.nota()).isEqualByComparingTo("100");
        assertThat(r.coberturaTecnica()).isEqualByComparingTo("0.125");
        assertThat(r.elegivelGlobal()).isFalse();
    }
    @Test void evidenciaAusenteDeOutraVersaoOuFuturaNaoContaComoAvaliacao() {
        var r = avaliar(List.of(
            evidencia(Controle.SEG01_ACESSO_POR_PERFIL, Situacao.ATENDIDO, "", "release-1", corte),
            evidencia(Controle.SEG02_ISOLAMENTO, Situacao.ATENDIDO, "teste", "antiga", corte),
            evidencia(Controle.SEG03_REVOGACAO, Situacao.ATENDIDO, "teste", "release-1", corte.plusSeconds(1)),
            evidencia(Controle.SEG04_FONTES_RAG, Situacao.NAO_VERIFICADO, "teste", "release-1", corte)
        ), "release-1", corte);
        assertThat(r.nota()).isNull();
        assertThat(r.amostra()).isZero();
    }
    @Test void duplicarUmControleNaoPodeAumentarCobertura() {
        var e = evidencia(Controle.SEG01_ACESSO_POR_PERFIL, Situacao.ATENDIDO, "teste", "release-1", corte);
        assertThatThrownBy(() -> avaliar(List.of(e,e), "release-1", corte)).isInstanceOf(IllegalArgumentException.class);
    }
}
