package br.com.hestia.autenticacao.service;

import br.com.hestia.autenticacao.dto.LoginDTO;
import br.com.hestia.autenticacao.dto.SessaoResponseDTO;
import br.com.hestia.autenticacao.model.SessaoUsuario;
import br.com.hestia.autenticacao.repository.SessaoUsuarioRepository;
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
public class AutenticacaoService {
    private static final Duration DURACAO_SESSAO = Duration.ofHours(8);
    private final UsuarioRepository usuarioRepository;
    private final SessaoUsuarioRepository sessaoRepository;
    private final PasswordEncoder passwordEncoder;

    public AutenticacaoService(UsuarioRepository usuarioRepository,
                               SessaoUsuarioRepository sessaoRepository,
                               PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.sessaoRepository = sessaoRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public SessaoResponseDTO autenticar(LoginDTO dto) {
        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(dto.email())
                .filter(u -> Boolean.TRUE.equals(u.getAtivo()))
                .filter(u -> passwordEncoder.matches(dto.senha(), u.getSenha()))
                .orElseThrow(() -> new CredenciaisInvalidasException("E-mail ou senha inválidos"));
        String token = UUID.randomUUID() + "." + UUID.randomUUID();
        LocalDateTime agora = LocalDateTime.now();
        SessaoUsuario sessao = new SessaoUsuario(usuario, hash(token), agora, agora.plus(DURACAO_SESSAO));
        sessaoRepository.save(sessao);
        return resposta(token, sessao);
    }

    @Transactional(readOnly = true)
    public SessaoUsuario validar(String token) {
        if (token == null || token.isBlank()) {
            throw new CredenciaisInvalidasException("Token de acesso ausente");
        }
        return sessaoRepository.findByTokenHash(hash(token))
                .filter(sessao -> sessao.ativaEm(LocalDateTime.now()))
                .filter(sessao -> Boolean.TRUE.equals(sessao.getUsuario().getAtivo()))
                .orElseThrow(() -> new CredenciaisInvalidasException("Sessão inválida ou expirada"));
    }

    @Transactional
    public void logout(String token) {
        SessaoUsuario sessao = validar(token);
        sessao.revogar(LocalDateTime.now());
    }

    public SessaoResponseDTO resposta(String token, SessaoUsuario sessao) {
        Usuario usuario = sessao.getUsuario();
        return new SessaoResponseDTO(token, sessao.getExpiraEm(), usuario.getId(), usuario.getNome(), usuario.getPerfil());
    }

    private String hash(String valor) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(valor.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 indisponível", exception);
        }
    }
}
