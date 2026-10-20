package br.com.hestia.autenticacao;

import br.com.hestia.autenticacao.model.RecuperacaoSenha;
import br.com.hestia.autenticacao.repository.RecuperacaoSenhaRepository;
import br.com.hestia.autenticacao.service.RecuperacaoSenhaService;
import br.com.hestia.usuario.model.Usuario;
import br.com.hestia.usuario.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RecuperacaoSenhaServiceTest {
    @Test
    void deveGerarTokenParaUsuarioAtivoESalvarApenasHash() {
        UsuarioRepository usuarios = mock(UsuarioRepository.class);
        RecuperacaoSenhaRepository recuperacoes = mock(RecuperacaoSenhaRepository.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        Usuario usuario = new Usuario();
        usuario.setAtivo(true);
        when(usuarios.findByEmailIgnoreCase("user@hestia.com")).thenReturn(Optional.of(usuario));

        RecuperacaoSenhaService service = new RecuperacaoSenhaService(usuarios, recuperacoes, encoder);
        String token = service.solicitar("user@hestia.com");

        assertNotNull(token);
        verify(recuperacoes).save(argThat(item ->
                item.getTokenHash() != null
                        && !item.getTokenHash().equals(token)
                        && item.getExpiraEm() != null));
    }

    @Test
    void deveAlterarSenhaEInvalidarToken() {
        UsuarioRepository usuarios = mock(UsuarioRepository.class);
        RecuperacaoSenhaRepository recuperacoes = mock(RecuperacaoSenhaRepository.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        Usuario usuario = new Usuario();
        usuario.setAtivo(true);
        when(usuarios.findByEmailIgnoreCase("user@hestia.com")).thenReturn(Optional.of(usuario));
        when(encoder.encode("novaSenha123")).thenReturn("hash-nova-senha");

        RecuperacaoSenhaService service = new RecuperacaoSenhaService(usuarios, recuperacoes, encoder);
        String token = service.solicitar("user@hestia.com");
        RecuperacaoSenha recuperacao = capturar(recuperacoes);
        when(recuperacoes.findByTokenHash(recuperacao.getTokenHash())).thenReturn(Optional.of(recuperacao));

        service.redefinir(token, "novaSenha123");

        assertEquals("hash-nova-senha", usuario.getSenha());
        assertNotNull(recuperacao.getExpiraEm());
        verify(encoder).encode("novaSenha123");
    }

    @Test
    void deveRejeitarTokenInvalidoSemAlterarSenha() {
        RecuperacaoSenhaRepository recuperacoes = mock(RecuperacaoSenhaRepository.class);
        when(recuperacoes.findByTokenHash(anyString())).thenReturn(Optional.empty());
        RecuperacaoSenhaService service = new RecuperacaoSenhaService(
                mock(UsuarioRepository.class), recuperacoes, mock(PasswordEncoder.class));

        assertThrows(IllegalArgumentException.class, () -> service.redefinir("token-invalido", "novaSenha123"));
    }

    @Test
    void deveInvalidarTokenDepoisDoPrimeiroUso() {
        UsuarioRepository usuarios = mock(UsuarioRepository.class);
        RecuperacaoSenhaRepository recuperacoes = mock(RecuperacaoSenhaRepository.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        Usuario usuario = new Usuario();
        usuario.setAtivo(true);
        when(usuarios.findByEmailIgnoreCase("user@hestia.com")).thenReturn(Optional.of(usuario));
        when(encoder.encode(anyString())).thenReturn("hash-nova-senha");

        RecuperacaoSenhaService service = new RecuperacaoSenhaService(usuarios, recuperacoes, encoder);
        String token = service.solicitar("user@hestia.com");
        RecuperacaoSenha recuperacao = capturar(recuperacoes);
        when(recuperacoes.findByTokenHash(recuperacao.getTokenHash())).thenReturn(Optional.of(recuperacao));

        service.redefinir(token, "novaSenha123");
        assertThrows(IllegalArgumentException.class, () -> service.redefinir(token, "outraSenha123"));
        verify(encoder, times(1)).encode("novaSenha123");
    }

    @SuppressWarnings("unchecked")
    private RecuperacaoSenha capturar(RecuperacaoSenhaRepository repository) {
        var captor = org.mockito.ArgumentCaptor.forClass(RecuperacaoSenha.class);
        verify(repository).save(captor.capture());
        return captor.getValue();
    }
}