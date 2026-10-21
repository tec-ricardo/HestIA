package br.com.hestia.indicadores.controller;

import br.com.hestia.empresa.model.Empresa;
import br.com.hestia.indicadores.service.ComposicaoMaturidadeService;
import br.com.hestia.indicadores.service.CoberturaMaturidadeService;
import br.com.hestia.indicadores.service.ConsultaCoberturaService;
import br.com.hestia.reutilizavel.repository.RespostaReutilizavelRepository;
import br.com.hestia.reutilizavel.service.AcessoRespostaService;
import br.com.hestia.usuario.model.PerfilUsuario;
import br.com.hestia.usuario.model.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class CoberturaMaturidadeControllerTest {

    private MockMvc mvc;
    private Usuario usuario;

    @BeforeEach
    void preparar() {
        var acesso = new AcessoRespostaService(
                mock(RespostaReutilizavelRepository.class)
        );

        var consulta = new ConsultaCoberturaService(
                new ComposicaoMaturidadeService(),
                new CoberturaMaturidadeService(),
                acesso
        );

        mvc = MockMvcBuilders.standaloneSetup(
                new CoberturaMaturidadeController(consulta)
        ).build();

        usuario = new Usuario();
        usuario.setId(10L);
        usuario.setAtivo(true);
        usuario.setPerfil(PerfilUsuario.FUNCIONARIO);
        usuario.setEmpresa(
                new Empresa(7L, "Empresa", "12345678901234", null, null)
        );
    }

    @Test
    void deveInformarAusenciaDeFontesSemInventarScore() throws Exception {
        mvc.perform(get("/indicadores/maturidade/cobertura")
                        .requestAttr("usuarioAutenticado", usuario)
                        .param("inicio", "2026-10-01")
                        .param("fim", "2026-10-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.empresaId").value(7))
                .andExpect(jsonPath("$.situacao")
                        .value("SEM_FONTES_DE_NOTAS_IMPLEMENTADAS"))
                .andExpect(jsonPath("$.cobertura.versaoRegra").value("1.0"))
                .andExpect(jsonPath("$.cobertura.periodoReferencia.inicio")
                        .value("2026-10-01"))
                .andExpect(jsonPath("$.cobertura.periodoReferencia.fim")
                        .value("2026-10-31"))
                .andExpect(jsonPath("$.cobertura.totalFatores").value(8))
                .andExpect(jsonPath("$.cobertura.fatoresDisponiveis").value(0))
                .andExpect(jsonPath("$.cobertura.coberturaPercentual").value(0.0))
                .andExpect(jsonPath("$.cobertura.score").doesNotExist());
    }

    @Test
    void deveExigirPeriodo() throws Exception {
        mvc.perform(get("/indicadores/maturidade/cobertura")
                        .requestAttr("usuarioAutenticado", usuario))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deveRejeitarPeriodoInvertido() throws Exception {
        mvc.perform(get("/indicadores/maturidade/cobertura")
                        .requestAttr("usuarioAutenticado", usuario)
                        .param("inicio", "2026-10-31")
                        .param("fim", "2026-10-01"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deveBloquearUsuarioInativo() throws Exception {
        usuario.setAtivo(false);

        mvc.perform(get("/indicadores/maturidade/cobertura")
                        .requestAttr("usuarioAutenticado", usuario)
                        .param("inicio", "2026-10-01")
                        .param("fim", "2026-10-31"))
                .andExpect(status().isForbidden());
    }
}