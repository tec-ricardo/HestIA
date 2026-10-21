package br.com.hestia.indicadores;

import br.com.hestia.autenticacao.config.ControleAcessoInterceptor;
import br.com.hestia.autenticacao.contexto.EmpresaAtual;
import br.com.hestia.autenticacao.service.*;
import br.com.hestia.indicadores.controller.ConfiancaIndicadoresController;
import br.com.hestia.indicadores.dto.*;
import br.com.hestia.indicadores.service.*;
import br.com.hestia.usuario.model.Usuario;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.math.BigDecimal;
import java.time.*;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ConfiancaIndicadoresControllerTest {
    @Test void deveExigirSessaoAntesDeConsultar() throws Exception {
        var service = mock(ConsultaConfiancaIndicadoresService.class);
        var auth = mock(AutenticacaoService.class);
        when(auth.validar(null)).thenThrow(new CredenciaisInvalidasException("Sessão obrigatória"));
        var mvc = MockMvcBuilders.standaloneSetup(new ConfiancaIndicadoresController(service))
                .addInterceptors(new ControleAcessoInterceptor(auth, new EmpresaAtual())).build();
        mvc.perform(get("/indicadores/confiabilidade").param("inicio", "2026-01-01")
                .param("fim", "2026-01-31")).andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
    }

    @Test void deveValidarPeriodoEEnumAntesDoServico() throws Exception {
        var service = mock(ConsultaConfiancaIndicadoresService.class);
        var mvc = MockMvcBuilders.standaloneSetup(new ConfiancaIndicadoresController(service)).build();
        mvc.perform(get("/indicadores/confiabilidade").requestAttr("usuarioAutenticado", new Usuario()))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/indicadores/confiabilidade").requestAttr("usuarioAutenticado", new Usuario())
                .param("inicio", "invalida").param("fim", "2026-01-31")).andExpect(status().isBadRequest());
        mvc.perform(get("/indicadores/confiabilidade").requestAttr("usuarioAutenticado", new Usuario())
                .param("inicio", "2026-01-01").param("fim", "2026-01-31").param("visao", "OUTRA"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test void deveExporContratoENaoGuardarResultadosEmCache() throws Exception {
        var service = mock(ConsultaConfiancaIndicadoresService.class);
        var usuario = new Usuario();
        var inicio = LocalDate.of(2026, 1, 1);
        var fim = LocalDate.of(2026, 1, 31);
        var confianca = new RegraConfiancaService().avaliar(null, 0, 0, 0, 0, false);
        var dto = new IndicadoresConfiancaDTO(1L, "PESSOAL", 7L, null,
                new PeriodoReferenciaDTO(inicio, fim), LocalDateTime.now(), "OPERACIONAL",
                "1.0", RegraConfiancaService.VERSAO, null, null, BigDecimal.ZERO,
                List.of(new IndicadoresConfiancaDTO.Indicador("usoResponsavel", "Uso Responsável",
                        new BigDecimal("0.15"), null, "PONTOS_0_100", "OPERACIONAL", null, null, null, confianca)),
                List.of("Sem dados não significa zero."));
        when(service.consultar(usuario, inicio, fim, ConsultaConfiancaIndicadoresService.Visao.PESSOAL, null)).thenReturn(dto);
        var mvc = MockMvcBuilders.standaloneSetup(new ConfiancaIndicadoresController(service)).build();
        mvc.perform(get("/indicadores/confiabilidade").requestAttr("usuarioAutenticado", usuario)
                .param("inicio", "2026-01-01").param("fim", "2026-01-31").param("empresaId", "999"))
                .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.empresaId").value(1))
                .andExpect(jsonPath("$.scoreGlobal").isEmpty())
                .andExpect(jsonPath("$.indicadores[0].confianca.situacao").value("SEM_DADOS"))
                .andExpect(jsonPath("$.indicadores[0].confianca.versaoRegra").value("CONFIANCA-1.0"));
        verify(service).consultar(usuario, inicio, fim, ConsultaConfiancaIndicadoresService.Visao.PESSOAL, null);
    }
}
