package br.com.hestia.indicadores.maturidade;

import br.com.hestia.usuario.model.Usuario;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

class ApuracaoMaturidadeServiceTest {

    private static final String FUSO_OPERACIONAL = "LOCAL_OPERACIONAL_SEM_CONVERSAO_ENTRE_FONTES";

    private final ObjectMapper json = new ObjectMapper();
    private final Usuario usuario = mock(Usuario.class);
    private final ApuracaoMaturidadeService service = spy(new ApuracaoMaturidadeService(null, null, null));

    @Test
    void calculaVariacoesQuandoConvencoesTemporaisCoincidem() throws Exception {
        var resultado = comparar(FUSO_OPERACIONAL);

        assertThat(resultado.get("comparavel")).isEqualTo(true);
        assertThat(resultado.get("variacaoGlobal")).isEqualTo(new BigDecimal("10"));
        assertThat(resultado.get("variacaoFatoresElegiveis")).isEqualTo(Map.of("F1", new BigDecimal("10")));
    }

    @Test
    void bloqueiaVariacoesQuandoConvencaoTemporalDivergeOuEstaAusente() throws Exception {
        for (String fuso : new String[]{"UTC", "", null}) {
            var resultado = comparar(fuso);

            assertThat(resultado.get("comparavel")).isEqualTo(false);
            assertThat(resultado.get("motivo")).isEqualTo("Convenções temporais de origem ausentes ou incompatíveis");
            assertThat(resultado.get("variacaoGlobal")).isNull();
            assertThat(resultado.get("variacaoFatoresElegiveis")).isEqualTo(Map.of());
        }
    }

    private Map<String, Object> comparar(String fusoAtual) throws Exception {
        doReturn(apuracao("2026-01-01", "2026-01-31", FUSO_OPERACIONAL, 40)).when(service).obter(usuario, 1L);
        doReturn(apuracao("2026-03-01", "2026-03-31", fusoAtual, 50)).when(service).obter(usuario, 2L);
        return service.evolucao(usuario, 1L, 2L);
    }

    private Map<String, Object> apuracao(String inicio, String fim, String fuso, int nota) throws Exception {
        Map<String, Object> resultado = new HashMap<>();
        resultado.put("inicio", inicio);
        resultado.put("fim", fim);
        resultado.put("naturezaDados", "OPERACIONAL");
        resultado.put("scoreGlobal", nota);
        resultado.put("fatores", List.of(Map.of("codigo", "F1", "elegivelGlobal", true, "nota", nota)));
        if (fuso != null) {
            resultado.put("fusoOrigem", fuso);
        }
        return Map.of("conteudo", json.writeValueAsString(Map.of("resultado", resultado)));
    }
}
