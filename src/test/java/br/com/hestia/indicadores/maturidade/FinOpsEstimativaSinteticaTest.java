package br.com.hestia.indicadores.maturidade;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

import static org.assertj.core.api.Assertions.assertThat;

class FinOpsEstimativaSinteticaTest {

    private final CalculadorasMaturidade calculadora = new CalculadorasMaturidade();
    private final BigDecimal custoBrl = new BigDecimal("7164.56");
    private final BigDecimal custoUsd = new BigDecimal("17.53");
    private final BigDecimal orcamentoSemMoeda = new BigDecimal("6357.74");

    @Test
    void comparaHipotesesDeMoedaComCambioSimulado() {
        var brlPorUsd = new BigDecimal("5.00");
        var custoConvertidoBrl = custoBrl.add(custoUsd.multiply(brlPorUsd));
        var custoConvertidoUsd = custoUsd.add(custoBrl.divide(brlPorUsd, MathContext.DECIMAL128));

        assertThat(custoConvertidoBrl).isEqualByComparingTo("7252.21");
        assertThat(custoConvertidoUsd.setScale(2, RoundingMode.HALF_UP)).isEqualByComparingTo("1450.44");

        var cenarioBrl = calculadora.finOps(orcamentoSemMoeda, custoConvertidoBrl, true, true, 1);
        var cenarioUsd = calculadora.finOps(orcamentoSemMoeda, custoConvertidoUsd, true, true, 1);
        assertThat(cenarioBrl.nota().setScale(2, RoundingMode.HALF_UP)).isEqualByComparingTo("85.93");
        assertThat(cenarioUsd.nota()).isEqualByComparingTo("100");
    }

    @Test
    void deixaNotaAusenteSemMoedaDoOrcamento() {
        var resultado = calculadora.finOps(orcamentoSemMoeda, null, false, true, 1);

        assertThat(resultado.estado()).isEqualTo("CONFIGURACAO_PENDENTE");
        assertThat(resultado.nota()).isNull();
        assertThat(resultado.elegivelGlobal()).isFalse();
    }
}
