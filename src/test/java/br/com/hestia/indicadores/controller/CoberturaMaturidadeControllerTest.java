package br.com.hestia.indicadores.controller;

import br.com.hestia.indicadores.maturidade.FatorCalculado;
import br.com.hestia.indicadores.maturidade.ResultadoMaturidadeService;
import br.com.hestia.indicadores.service.ComposicaoMaturidadeService;
import br.com.hestia.indicadores.service.CoberturaMaturidadeService;
import br.com.hestia.indicadores.service.ConsultaCoberturaService;
import br.com.hestia.usuario.model.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class CoberturaMaturidadeControllerTest {

    private MockMvc mvc;
    private Usuario usuario;
    private ResultadoMaturidadeService resultados;
    private final ComposicaoMaturidadeService composicao = new ComposicaoMaturidadeService();

    @BeforeEach
    void preparar() {
        usuario = new Usuario();
        resultados = mock(ResultadoMaturidadeService.class);
        mvc = MockMvcBuilders.standaloneSetup(new CoberturaMaturidadeController(
                new ConsultaCoberturaService(composicao, resultados, new CoberturaMaturidadeService())
        )).build();
        doReturn(resultado(Set.of(), false)).when(resultados).consultar(same(usuario), any());
    }

    @Test
    void deveInformarAusenciaDeFatoresElegiveisSemInventarScore() throws Exception {
        mvc.perform(get("/indicadores/maturidade/cobertura")
                        .requestAttr("usuarioAutenticado", usuario)
                        .param("inicio", "2026-10-01").param("fim", "2026-10-21"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.empresaId").value(7))
                .andExpect(jsonPath("$.situacao").value("SEM_FATORES_ELEGIVEIS"))
                .andExpect(jsonPath("$.cobertura.versaoRegra").value("1.0"))
                .andExpect(jsonPath("$.cobertura.periodoReferencia.inicio").value("2026-10-01"))
                .andExpect(jsonPath("$.cobertura.periodoReferencia.fim").value("2026-10-21"))
                .andExpect(jsonPath("$.cobertura.totalFatores").value(8))
                .andExpect(jsonPath("$.cobertura.fatoresDisponiveis").value(0))
                .andExpect(jsonPath("$.cobertura.coberturaPercentual").value(0.0))
                .andExpect(jsonPath("$.cobertura.score").doesNotExist())
                .andExpect(jsonPath("$.cobertura.fatores[0].motivoAusencia").value("Amostra insuficiente"));
        verify(resultados).consultar(usuario, new ResultadoMaturidadeService.Consulta(
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 21),
                ResultadoMaturidadeService.Natureza.OPERACIONAL, null, null, null, null
        ));
    }

    @Test
    void deveContarFatoresElegiveisSemConfundirCoberturaComPeso() throws Exception {
        doReturn(resultado(Set.of("seguranca"), true)).when(resultados).consultar(same(usuario), any());
        mvc.perform(get("/indicadores/maturidade/cobertura")
                        .requestAttr("usuarioAutenticado", usuario)
                        .param("inicio", "2026-10-01").param("fim", "2026-10-21"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.situacao").value("PARCIAL"))
                .andExpect(jsonPath("$.cobertura.fatoresDisponiveis").value(1))
                .andExpect(jsonPath("$.cobertura.coberturaPercentual").value(12.5))
                .andExpect(jsonPath("$.cobertura.score").doesNotExist())
                .andExpect(jsonPath("$.cobertura.fatores[0].nota").value(75))
                .andExpect(jsonPath("$.cobertura.fatores[0].disponivel").value(false));
    }

    @Test
    void devePreservarScoreGlobalQuandoTodosOsFatoresForemElegiveis() throws Exception {
        var codigos = composicao.consultar().fatores().stream()
                .map(fator -> fator.codigo()).collect(java.util.stream.Collectors.toSet());
        doReturn(resultado(codigos, false)).when(resultados).consultar(same(usuario), any());
        mvc.perform(get("/indicadores/maturidade/cobertura")
                        .requestAttr("usuarioAutenticado", usuario)
                        .param("inicio", "2026-10-01").param("fim", "2026-10-21"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.situacao").value("COMPLETO"))
                .andExpect(jsonPath("$.cobertura.fatoresDisponiveis").value(8))
                .andExpect(jsonPath("$.cobertura.coberturaPercentual").value(100))
                .andExpect(jsonPath("$.cobertura.score").value(75));
    }

    @Test
    void deveEncaminharSelecaoSinteticaParaValidacaoDoServicoAutorizado() throws Exception {
        String hash = "a".repeat(64);
        mvc.perform(get("/indicadores/maturidade/cobertura")
                        .requestAttr("usuarioAutenticado", usuario)
                        .param("inicio", "2026-10-01").param("fim", "2026-10-21")
                        .param("fonte", "SINTETICA").param("snapshot", hash)
                        .param("departamentoId", "11").param("departamentoOrigem", "C01-D01")
                        .param("tipoIssue", "Bug"))
                .andExpect(status().isOk());
        verify(resultados).consultar(usuario, new ResultadoMaturidadeService.Consulta(
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 21),
                ResultadoMaturidadeService.Natureza.SINTETICA, hash, 11L, "C01-D01", "Bug"
        ));
    }

    @Test
    void devePreservarBloqueioDeEscopoDoServicoDeResultados() throws Exception {
        when(resultados.consultar(same(usuario), any())).thenThrow(
                new ResponseStatusException(HttpStatus.FORBIDDEN, "Departamento não autorizado")
        );
        mvc.perform(get("/indicadores/maturidade/cobertura")
                        .requestAttr("usuarioAutenticado", usuario)
                        .param("inicio", "2026-10-01").param("fim", "2026-10-21"))
                .andExpect(status().isForbidden());
    }

    @Test
    void deveExigirPeriodo() throws Exception {
        mvc.perform(get("/indicadores/maturidade/cobertura")
                        .requestAttr("usuarioAutenticado", usuario))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(resultados);
    }

    @Test
    void deveRejeitarPeriodoInvertido() throws Exception {
        mvc.perform(get("/indicadores/maturidade/cobertura")
                        .requestAttr("usuarioAutenticado", usuario)
                        .param("inicio", "2026-10-21").param("fim", "2026-10-01"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(resultados);
    }

    @Test
    void deveRejeitarVersaoDesconhecida() throws Exception {
        mvc.perform(get("/indicadores/maturidade/cobertura")
                        .requestAttr("usuarioAutenticado", usuario)
                        .param("inicio", "2026-10-01").param("fim", "2026-10-21")
                        .param("versao", "inexistente"))
                .andExpect(status().isNotFound());
        verifyNoInteractions(resultados);
    }

    private ResultadoMaturidadeService.Resultado resultado(Set<String> elegiveis, boolean notaParcial) {
        var fatores = composicao.consultar().fatores().stream().map(fator -> {
            boolean elegivel = elegiveis.contains(fator.codigo());
            return new FatorCalculado(
                    fator.codigo(), fator.peso(),
                    elegivel || notaParcial ? new BigDecimal("75") : null,
                    elegivel ? "CALCULADO" : "PARCIAL", elegivel,
                    elegivel ? "Evidência suficiente" : "Amostra insuficiente",
                    BigDecimal.ONE, BigDecimal.ONE, elegivel ? 30 : 10, 30L,
                    null, null, "Fórmula da fonte", "OPERACIONAL", Map.of(), List.of(), "teste"
            );
        }).toList();
        var resultado = mock(ResultadoMaturidadeService.Resultado.class);
        when(resultado.empresaId()).thenReturn(7L);
        when(resultado.versaoPesos()).thenReturn("1.0");
        when(resultado.fatores()).thenReturn(fatores);
        when(resultado.scoreGlobal()).thenReturn(elegiveis.size() == 8 ? new BigDecimal("75") : null);
        return resultado;
    }
}

