package br.com.hestia.gamificacao.model;

import br.com.hestia.usuario.model.Usuario;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity @Table(name = "progressos_usuario")
public class ProgressoUsuario {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false, unique = true) private Usuario usuario;
    @Column(name = "xp_total", nullable = false) private int xpTotal;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) private NivelExperiencia nivel;
    @Column(name = "atualizado_em", nullable = false) private LocalDateTime atualizadoEm;
    protected ProgressoUsuario() {}
    public ProgressoUsuario(Usuario usuario) { this.usuario = usuario; this.nivel = NivelExperiencia.APRENDIZ; this.atualizadoEm = LocalDateTime.now(); }
    public void adicionarXp(int pontos) { if (pontos < 0) throw new IllegalArgumentException("XP não pode ser negativo"); xpTotal += pontos; nivel = NivelExperiencia.paraXp(xpTotal); atualizadoEm = LocalDateTime.now(); }
    public Long getId() { return id; } public Usuario getUsuario() { return usuario; }
    public int getXpTotal() { return xpTotal; } public NivelExperiencia getNivel() { return nivel; }
    public LocalDateTime getAtualizadoEm() { return atualizadoEm; }
}
