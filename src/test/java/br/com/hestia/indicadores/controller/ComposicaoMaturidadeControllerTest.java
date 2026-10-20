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

class ComposicaoMaturidadeControllerTest {

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
    void deveRetornarComposicaoComPesosDoProjeto() throws Exception {
        mvc.perform(get("/indicadores/maturidade/composicao")
                        .requestAttr("usuarioAutenticado", usuario))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notaMinima").value(0))
                .andExpect(jsonPath("$.notaMaxima").value(100))
                .andExpect(jsonPath("$.somaPesos").value(1.0))
                .andExpect(jsonPath("$.fatores.length()").value(8))
                .andExpect(jsonPath("$.fatores[0].codigo").value("aiLiteracy"))
                .andExpect(jsonPath("$.fatores[0].peso").value(0.15))
                .andExpect(jsonPath("$.fatores[1].codigo").value("governanca"))
                .andExpect(jsonPath("$.fatores[1].peso").value(0.15))
                .andExpect(jsonPath("$.fatores[2].codigo").value("usoResponsavel"))
                .andExpect(jsonPath("$.fatores[2].peso").value(0.15))
                .andExpect(jsonPath("$.fatores[3].codigo").value("produtividade"))
                .andExpect(jsonPath("$.fatores[3].peso").value(0.10))
                .andExpect(jsonPath("$.fatores[4].codigo").value("finOps"))
                .andExpect(jsonPath("$.fatores[4].peso").value(0.10))
                .andExpect(jsonPath("$.fatores[5].codigo").value("seguranca"))
                .andExpect(jsonPath("$.fatores[5].peso").value(0.20))
                .andExpect(jsonPath("$.fatores[6].codigo").value("colaboracao"))
                .andExpect(jsonPath("$.fatores[6].peso").value(0.05))
                .andExpect(jsonPath("$.fatores[7].codigo")
                        .value("desenvolvimentoCompetencias"))
                .andExpect(jsonPath("$.fatores[7].peso").value(0.10))
                .andExpect(jsonPath("$.formula").isNotEmpty())
                .andExpect(jsonPath("$.score").doesNotExist());
    }

    @Test
    void deveBloquearUsuarioInativo() throws Exception {
        usuario.setAtivo(false);

        mvc.perform(get("/indicadores/maturidade/composicao")
                        .requestAttr("usuarioAutenticado", usuario))
                .andExpect(status().isForbidden());
    }

    @Test
    void deveBloquearUsuarioSemEmpresa() throws Exception {
        usuario.setEmpresa(null);

        mvc.perform(get("/indicadores/maturidade/composicao")
                        .requestAttr("usuarioAutenticado", usuario))
                .andExpect(status().isForbidden());
    }
}