package br.com.hestia.autenticacao.model;

import br.com.hestia.usuario.model.Usuario;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "recuperacoes_senha", indexes = @Index(name = "idx_recuperacao_token_hash", columnList = "token_hash", unique = true))
public class RecuperacaoSenha {
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
    @Column(name = "utilizada_em")
    private LocalDateTime utilizadaEm;

    protected RecuperacaoSenha() {}
    public RecuperacaoSenha(Usuario usuario, String tokenHash, LocalDateTime criadaEm, LocalDateTime expiraEm) {
        this.usuario = usuario; this.tokenHash = tokenHash; this.criadaEm = criadaEm; this.expiraEm = expiraEm;
    }
    public boolean validaEm(LocalDateTime instante) { return utilizadaEm == null && expiraEm.isAfter(instante); }
    public void utilizar(LocalDateTime instante) { this.utilizadaEm = instante; }
    public Usuario getUsuario() { return usuario; }
    public String getTokenHash() { return tokenHash; }
    public LocalDateTime getExpiraEm() { return expiraEm; }
}