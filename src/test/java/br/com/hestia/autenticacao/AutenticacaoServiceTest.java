package br.com.hestia.autenticacao;

import br.com.hestia.autenticacao.dto.LoginDTO;
import br.com.hestia.autenticacao.model.SessaoUsuario;
import br.com.hestia.autenticacao.repository.SessaoUsuarioRepository;
import br.com.hestia.autenticacao.service.AutenticacaoService;
import br.com.hestia.autenticacao.service.CredenciaisInvalidasException;
import br.com.hestia.usuario.model.PerfilUsuario;
import br.com.hestia.usuario.model.Usuario;
import br.com.hestia.usuario.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AutenticacaoServiceTest {
    @Mock UsuarioRepository usuarioRepository;
    @Mock SessaoUsuarioRepository sessaoRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock Usuario usuario;
    private AutenticacaoService service;

    @BeforeEach void setUp() { service = new AutenticacaoService(usuarioRepository, sessaoRepository, passwordEncoder); }

    @Test
    void deveAutenticarUsuarioAtivoComSenhaValida() {
        when(usuarioRepository.findByEmailIgnoreCase("ricardo@hestia.com")).thenReturn(Optional.of(usuario));
        when(usuario.getAtivo()).thenReturn(true);
        when(usuario.getSenha()).thenReturn("hash");
        when(passwordEncoder.matches("segredo", "hash")).thenReturn(true);
        when(usuario.getId()).thenReturn(1L);
        when(usuario.getNome()).thenReturn("Ricardo");
        when(usuario.getPerfil()).thenReturn(PerfilUsuario.ADMIN);

        var resposta = service.autenticar(new LoginDTO("ricardo@hestia.com", "segredo"));

        assertNotNull(resposta.token());
        assertEquals(PerfilUsuario.ADMIN, resposta.perfil());
        verify(sessaoRepository).save(any(SessaoUsuario.class));
    }

    @Test
    void deveOcultarMotivoDeCredencialInvalida() {
        when(usuarioRepository.findByEmailIgnoreCase("x@hestia.com")).thenReturn(Optional.empty());
        var erro = assertThrows(CredenciaisInvalidasException.class,
                () -> service.autenticar(new LoginDTO("x@hestia.com", "errada")));
        assertEquals("E-mail ou senha inválidos", erro.getMessage());
        verifyNoInteractions(sessaoRepository);
    }
}
