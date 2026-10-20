package br.com.hestia.indicadores.controller;

import br.com.hestia.empresa.model.Empresa;
import br.com.hestia.indicadores.service.ComposicaoMaturidadeService;
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

class PeriodoRegraMaturidadeTest {

    private MockMvc mvc;
    private Usuario usuario;

    @BeforeEach
    void preparar() {
        var acesso = new AcessoRespostaService(
                mock(RespostaReutilizavelRepository.class)
        );

        mvc = MockMvcBuilders.standaloneSetup(
                new ComposicaoMaturidadeController(
                        new ComposicaoMaturidadeService(),
                        acesso
                )
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
    void deveRetornarPeriodoEVersaoSolicitados() throws Exception {
        mvc.perform(get("/indicadores/maturidade/composicao")
                        .requestAttr("usuarioAutenticado", usuario)
                        .param("inicio", "2026-10-01")
                        .param("fim", "2026-10-31")
                        .param("versao", "1.0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.versaoRegra").value("1.0"))
                .andExpect(jsonPath("$.periodoReferencia.inicio")
                        .value("2026-10-01"))
                .andExpect(jsonPath("$.periodoReferencia.fim")
                        .value("2026-10-31"))
                .andExpect(jsonPath("$.somaPesos").value(1.0));
    }

    @Test
    void deveManterConsultaSemPeriodo() throws Exception {
        mvc.perform(get("/indicadores/maturidade/composicao")
                        .requestAttr("usuarioAutenticado", usuario))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.versaoRegra").value("1.0"))
                .andExpect(jsonPath("$.periodoReferencia").doesNotExist())
                .andExpect(jsonPath("$.fatores.length()").value(8));
    }

    @Test
    void deveAceitarPeriodoDeUmDia() throws Exception {
        mvc.perform(get("/indicadores/maturidade/composicao")
                        .requestAttr("usuarioAutenticado", usuario)
                        .param("inicio", "2026-10-01")
                        .param("fim", "2026-10-01"))
                .andExpect(status().isOk());
    }

    @Test
    void deveRejeitarPeriodoIncompleto() throws Exception {
        for (String parametro : new String[]{"inicio", "fim"}) {
            mvc.perform(get("/indicadores/maturidade/composicao")
                            .requestAttr("usuarioAutenticado", usuario)
                            .param(parametro, "2026-10-01"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Test
    void deveRejeitarPeriodoInvertido() throws Exception {
        mvc.perform(get("/indicadores/maturidade/composicao")
                        .requestAttr("usuarioAutenticado", usuario)
                        .param("inicio", "2026-10-31")
                        .param("fim", "2026-10-01"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deveRejeitarDataInvalida() throws Exception {
        mvc.perform(get("/indicadores/maturidade/composicao")
                        .requestAttr("usuarioAutenticado", usuario)
                        .param("inicio", "data-invalida")
                        .param("fim", "2026-10-31"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deveRejeitarVersaoDesconhecida() throws Exception {
        mvc.perform(get("/indicadores/maturidade/composicao")
                        .requestAttr("usuarioAutenticado", usuario)
                        .param("versao", "9.0"))
                .andExpect(status().isNotFound());
    }
}