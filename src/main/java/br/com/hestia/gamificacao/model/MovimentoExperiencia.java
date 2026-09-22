package br.com.hestia.gamificacao.model;

import br.com.hestia.usuario.model.Usuario;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity @Table(name = "movimentos_experiencia", uniqueConstraints =
        @UniqueConstraint(name = "uk_movimento_referencia", columnNames = {"usuario_id","referencia"}))
public class MovimentoExperiencia {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "usuario_id", nullable = false) private Usuario usuario;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 40) private AcaoExperiencia acao;
    @Column(nullable = false) private int pontos;
    @Column(nullable = false, length = 150) private String referencia;
    @Column(name = "data_hora", nullable = false) private LocalDateTime dataHora;
    protected MovimentoExperiencia() {}
    public MovimentoExperiencia(Usuario usuario, AcaoExperiencia acao, int pontos, String referencia) {
        this.usuario = usuario; this.acao = acao; this.pontos = pontos; this.referencia = referencia; this.dataHora = LocalDateTime.now();
    }
}
