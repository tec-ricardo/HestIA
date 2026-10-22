package br.com.hestia.indicadores;

import br.com.hestia.indicadores.service.RegraConfiancaService;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;

class RegraConfiancaServiceTest {
    private final RegraConfiancaService regra = new RegraConfiancaService();

    @Test void deveSepararCompletoParcialEstimadoEAusente() {
        assertEquals("CONFIAVEL", regra.avaliar(BigDecimal.ZERO, 30, 30, 30, 30, false).classificacao());
        assertEquals("PARCIAL", regra.avaliar(BigDecimal.TEN, 30, 29, 29, 29, false).classificacao());
        assertEquals("ESTIMADO", regra.avaliar(BigDecimal.TEN, 30, 30, 30, 30, true).classificacao());
        var vazio = regra.avaliar(null, 0, 0, 0, 0, false);
        assertEquals("SEM_DADOS", vazio.situacao());
        assertNull(vazio.cobertura());
        assertNull(vazio.completude());
        assertEquals("FONTE_PENDENTE", regra.indisponivel("Sem fonte").situacao());
    }
    @Test void deveRespeitarLimitesExatosSemPromocaoPorArredondamento() {
        assertEquals("CONFIAVEL", regra.avaliar(BigDecimal.TEN, 40, 38, 38, 38, false).classificacao());
        assertEquals("PARCIAL", regra.avaliar(BigDecimal.TEN, 40, 37, 37, 37, false).classificacao());
        assertEquals("PARCIAL", regra.avaliar(BigDecimal.TEN, 40, 40, 37, 37, false).classificacao());
        assertEquals("PARCIAL", regra.avaliar(BigDecimal.TEN, 10000001, 9500000, 9500000, 9500000, false).classificacao());
    }
    @Test void deveRecusarContagensImpossiveis() {
        assertThrows(IllegalArgumentException.class, () -> regra.avaliar(BigDecimal.ONE, 0, 0, 0, 0, false));
        assertThrows(IllegalArgumentException.class, () -> regra.avaliar(null, -1, 0, 0, 0, false));
        assertThrows(IllegalArgumentException.class, () -> regra.avaliar(null, 10, 11, 0, 0, false));
        assertThrows(IllegalArgumentException.class, () -> regra.avaliar(null, 10, 9, 10, 0, false));
        assertThrows(IllegalArgumentException.class, () -> regra.avaliar(null, 10, 9, 9, 10, false));
    }
}
