package br.com.hestia.autenticacao.model;

import br.com.hestia.usuario.model.Usuario;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "sessoes_usuario", indexes = @Index(name = "idx_sessao_token", columnList = "token_hash", unique = true))
public class SessaoUsuario {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;
    @Column(name = "token_hash", nullable = false, length = 64, unique = true)
    private String tokenHash;
    @Column(name = "criada_em", nullable = false)
    private LocalDateTime criadaEm;
    @Column(name = "expira_em", nullable = false)
    private LocalDateTime expiraEm;
    @Column(name = "revogada_em")
    private LocalDateTime revogadaEm;

    protected SessaoUsuario() {}
    public SessaoUsuario(Usuario usuario, String tokenHash, LocalDateTime criadaEm, LocalDateTime expiraEm) {
        this.usuario = usuario; this.tokenHash = tokenHash; this.criadaEm = criadaEm; this.expiraEm = expiraEm;
    }
    public boolean ativaEm(LocalDateTime instante) { return revogadaEm == null && expiraEm.isAfter(instante); }
    public void revogar(LocalDateTime instante) { this.revogadaEm = instante; }
    public Long getId() { return id; }
    public Usuario getUsuario() { return usuario; }
    public String getTokenHash() { return tokenHash; }
    public LocalDateTime getCriadaEm() { return criadaEm; }
    public LocalDateTime getExpiraEm() { return expiraEm; }
    public LocalDateTime getRevogadaEm() { return revogadaEm; }
}
