package br.com.hestia.rag.controller;

import br.com.hestia.empresa.model.Empresa;
import br.com.hestia.rag.service.BuscaVetorialService;
import br.com.hestia.reutilizavel.repository.RespostaReutilizavelRepository;
import br.com.hestia.reutilizavel.service.AcessoRespostaService;
import br.com.hestia.usuario.model.PerfilUsuario;
import br.com.hestia.usuario.model.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class BuscaSemelhanteControllerTest {

    private BuscaVetorialService busca;
    private MockMvc mvc;
    private Usuario usuario;

    @BeforeEach
    void preparar() {
        busca = mock(BuscaVetorialService.class);

        var acesso = new AcessoRespostaService(
                mock(RespostaReutilizavelRepository.class)
        );

        mvc = MockMvcBuilders.standaloneSetup(
                new BuscaSemelhanteController(busca, acesso)
        ).build();

        usuario = new Usuario();
        usuario.setId(10L);
        usuario.setAtivo(true);
        usuario.setPerfil(PerfilUsuario.FUNCIONARIO);
        usuario.setEmpresa(
                new Empresa(7L, "Empresa de teste", "12345678901234", null, null)
        );
    }

    @Test
    void deveUsarEmpresaDaSessaoEParametrosInformados() throws Exception {
        when(busca.buscarSemelhantes(7L, "Pergunta", 3, 0.85))
                .thenReturn(List.of());

        mvc.perform(post("/api/respostas-reutilizaveis/buscar-semelhantes")
                        .requestAttr("usuarioAutenticado", usuario)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "pergunta": "Pergunta",
                                  "limite": 3,
                                  "similaridadeMinima": 0.85
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));

        verify(busca).buscarSemelhantes(7L, "Pergunta", 3, 0.85);
    }

    @Test
    void deveAplicarValoresPadrao() throws Exception {
        when(busca.buscarSemelhantes(7L, "Pergunta", 5, 0.70))
                .thenReturn(List.of());

        mvc.perform(post("/api/respostas-reutilizaveis/buscar-semelhantes")
                        .requestAttr("usuarioAutenticado", usuario)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"pergunta": "Pergunta"}
                                """))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));

        verify(busca).buscarSemelhantes(7L, "Pergunta", 5, 0.70);
    }

    @Test
    void deveRejeitarPerguntaVazia() throws Exception {
        mvc.perform(post("/api/respostas-reutilizaveis/buscar-semelhantes")
                        .requestAttr("usuarioAutenticado", usuario)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"pergunta": " "}
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(busca);
    }

    @Test
    void deveRejeitarLimiteESimilaridadeInvalidos() throws Exception {
        for (String corpo : List.of(
                """
                {"pergunta":"Pergunta","limite":0}
                """,
                """
                {"pergunta":"Pergunta","limite":21}
                """,
                """
                {"pergunta":"Pergunta","similaridadeMinima":-0.1}
                """,
                """
                {"pergunta":"Pergunta","similaridadeMinima":1.1}
                """
        )) {
            mvc.perform(post("/api/respostas-reutilizaveis/buscar-semelhantes")
                            .requestAttr("usuarioAutenticado", usuario)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(corpo))
                    .andExpect(status().isBadRequest());
        }

        verifyNoInteractions(busca);
    }

    @Test
    void deveBloquearUsuarioInativo() throws Exception {
        usuario.setAtivo(false);

        mvc.perform(post("/api/respostas-reutilizaveis/buscar-semelhantes")
                        .requestAttr("usuarioAutenticado", usuario)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"pergunta":"Pergunta"}
                                """))
                .andExpect(status().isForbidden());

        verifyNoInteractions(busca);
    }
}