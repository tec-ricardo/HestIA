package br.com.hestia.autenticacao;

import br.com.hestia.autenticacao.config.ControleAcessoInterceptor;
import br.com.hestia.autenticacao.model.SessaoUsuario;
import br.com.hestia.autenticacao.service.AutenticacaoService;
import br.com.hestia.usuario.model.PerfilUsuario;
import br.com.hestia.usuario.model.Usuario;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ControleAcessoInterceptorTest {
    @Test
    void funcionarioNaoPodeAcessarAdministracaoDeUsuarios() throws Exception {
        AutenticacaoService service = mock(AutenticacaoService.class);
        SessaoUsuario sessao = mock(SessaoUsuario.class);
        Usuario usuario = mock(Usuario.class);
        when(service.validar("token")).thenReturn(sessao);
        when(sessao.getUsuario()).thenReturn(usuario);
        when(usuario.getPerfil()).thenReturn(PerfilUsuario.FUNCIONARIO);
        var request = new MockHttpServletRequest("GET", "/usuarios");
        request.addHeader("Authorization", "Bearer token");
        var response = new MockHttpServletResponse();

        boolean permitido = new ControleAcessoInterceptor(service).preHandle(request, response, new Object());

        assertFalse(permitido);
        assertEquals(403, response.getStatus());
    }

    @Test
    void gestorPodeAvaliarConformidade() throws Exception {
        AutenticacaoService service = mock(AutenticacaoService.class);
        SessaoUsuario sessao = mock(SessaoUsuario.class);
        Usuario usuario = mock(Usuario.class);
        when(service.validar("token")).thenReturn(sessao);
        when(sessao.getUsuario()).thenReturn(usuario);
        when(usuario.getPerfil()).thenReturn(PerfilUsuario.GESTOR);
        var request = new MockHttpServletRequest("POST", "/avaliacoes-conformidade");
        request.addHeader("Authorization", "Bearer token");

        assertTrue(new ControleAcessoInterceptor(service)
                .preHandle(request, new MockHttpServletResponse(), new Object()));
    }
}
