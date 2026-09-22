package br.com.hestia.gamificacao.model;

import br.com.hestia.usuario.model.Usuario;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity @Table(name = "progressos_missao", uniqueConstraints =
        @UniqueConstraint(name = "uk_progresso_missao", columnNames = {"usuario_id","missao_id"}))
public class ProgressoMissao {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "usuario_id", nullable = false) private Usuario usuario;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "missao_id", nullable = false) private Missao missao;
    @Column(nullable = false) private int progresso;
    @Column(name = "concluida_em") private LocalDateTime concluidaEm;
    @Column(name = "recompensa_concedida", nullable = false) private boolean recompensaConcedida;
    protected ProgressoMissao() {}
    public ProgressoMissao(Usuario usuario, Missao missao) { this.usuario = usuario; this.missao = missao; }
    public boolean incrementar() { if (concluidaEm != null) return false; progresso++; if (progresso >= missao.getMetaQuantidade()) { concluidaEm = LocalDateTime.now(); return true; } return false; }
    public void marcarRecompensaConcedida() { recompensaConcedida = true; }
    public Missao getMissao() { return missao; } public int getProgresso() { return progresso; }
    public LocalDateTime getConcluidaEm() { return concluidaEm; } public boolean isRecompensaConcedida() { return recompensaConcedida; }
}
