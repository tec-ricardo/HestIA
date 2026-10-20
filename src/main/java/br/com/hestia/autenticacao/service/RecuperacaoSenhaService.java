package br.com.hestia.autenticacao.service;

import br.com.hestia.autenticacao.model.RecuperacaoSenha;
import br.com.hestia.autenticacao.repository.RecuperacaoSenhaRepository;
import br.com.hestia.usuario.model.Usuario;
import br.com.hestia.usuario.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class RecuperacaoSenhaService {
    private static final Duration DURACAO_TOKEN = Duration.ofMinutes(30);
    private final UsuarioRepository usuarioRepository;
    private final RecuperacaoSenhaRepository recuperacaoRepository;
    private final PasswordEncoder passwordEncoder;

    public RecuperacaoSenhaService(UsuarioRepository usuarioRepository,
                                   RecuperacaoSenhaRepository recuperacaoRepository,
                                   PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.recuperacaoRepository = recuperacaoRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public String solicitar(String email) {
        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(email).orElse(null);
        if (usuario == null || !Boolean.TRUE.equals(usuario.getAtivo())) return null;

        String token = UUID.randomUUID() + "." + UUID.randomUUID();
        LocalDateTime agora = LocalDateTime.now();
        // Um novo pedido invalida tokens anteriores para impedir reutilização concorrente.
        recuperacaoRepository.invalidarTokensAtivos(usuario.getId(), agora);
        recuperacaoRepository.save(new RecuperacaoSenha(usuario, hash(token), agora, agora.plus(DURACAO_TOKEN)));
        return token;
    }

    @Transactional
    public void redefinir(String token, String novaSenha) {
        RecuperacaoSenha recuperacao = recuperacaoRepository.findByTokenHash(hash(token))
                .filter(item -> item.validaEm(LocalDateTime.now()))
                .orElseThrow(() -> new IllegalArgumentException("Token inválido ou expirado"));
        recuperacao.getUsuario().setSenha(passwordEncoder.encode(novaSenha));
        recuperacao.utilizar(LocalDateTime.now());
    }

    private String hash(String valor) {
        if (valor == null || valor.isBlank()) throw new IllegalArgumentException("Token ausente");
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(valor.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 indisponível", exception);
        }
    }
}