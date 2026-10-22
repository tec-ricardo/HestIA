package br.com.hestia.indicadores.service;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.Map;
import java.util.HashMap;
import static org.assertj.core.api.Assertions.*;

class CalculoMaturidadeServiceTest {
    private final CalculoMaturidadeService service = new CalculoMaturidadeService(new ComposicaoMaturidadeService());

    private Map<String, BigDecimal> exemplo() {
        return new HashMap<>(Map.of(
            "aiLiteracy", new BigDecimal("72"), "governanca", new BigDecimal("65"),
            "usoResponsavel", new BigDecimal("80"), "produtividade", new BigDecimal("76"),
            "finOps", new BigDecimal("58"), "seguranca", new BigDecimal("85"),
            "colaboracao", new BigDecimal("70"), "desenvolvimentoCompetencias", new BigDecimal("68")
        ));
    }

    @Test void reproduzExemploDaScrum62SemArredondarParcelas() {
        var r = service.calcular(exemplo());
        assertThat(r.score()).isEqualByComparingTo("73.25");
        assertThat(r.pesoComNota()).isEqualByComparingTo("1.00");
        assertThat(r.fatoresPendentes()).isEmpty();
    }

    @Test void ausenciaNaoViraZeroNemRedistribuiPesos() {
        var notas = exemplo(); notas.remove("produtividade");
        var r = service.calcular(notas);
        assertThat(r.score()).isNull();
        assertThat(r.pesoComNota()).isEqualByComparingTo("0.90");
        assertThat(r.fatoresPendentes()).containsExactly("produtividade");
        assertThat(service.calcular(Map.of()).parcelas()).hasSize(8);
    }

    @Test void preservaZeroValidoEExtremosDaEscala() {
        var notas = exemplo(); notas.replaceAll((k,v) -> BigDecimal.ZERO);
        assertThat(service.calcular(notas).score()).isEqualByComparingTo("0");
        notas.replaceAll((k,v) -> BigDecimal.valueOf(100));
        assertThat(service.calcular(notas).score()).isEqualByComparingTo("100");
    }

    @Test void rejeitaNotasForaDaEscalaEFatoresDesconhecidos() {
        var notas = exemplo(); notas.put("finOps", new BigDecimal("100.01"));
        assertThatThrownBy(() -> service.calcular(notas)).isInstanceOf(IllegalArgumentException.class);
        notas.put("finOps", new BigDecimal("-1"));
        assertThatThrownBy(() -> service.calcular(notas)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.calcular(Map.of("outro", BigDecimal.ONE))).isInstanceOf(IllegalArgumentException.class);
    }
}
