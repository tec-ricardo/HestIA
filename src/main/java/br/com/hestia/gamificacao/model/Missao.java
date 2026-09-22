package br.com.hestia.gamificacao.model;

import jakarta.persistence.*;

@Entity @Table(name = "missoes")
public class Missao {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, unique = true, length = 80) private String codigo;
    @Column(nullable = false, length = 150) private String titulo;
    @Column(nullable = false, length = 500) private String descricao;
    @Enumerated(EnumType.STRING) @Column(name = "acao_alvo", nullable = false, length = 40) private AcaoExperiencia acaoAlvo;
    @Column(name = "meta_quantidade", nullable = false) private int metaQuantidade;
    @Column(name = "recompensa_xp", nullable = false) private int recompensaXp;
    @Column(nullable = false) private boolean ativa = true;
    protected Missao() {}
    public Long getId() { return id; } public String getCodigo() { return codigo; } public String getTitulo() { return titulo; }
    public String getDescricao() { return descricao; } public AcaoExperiencia getAcaoAlvo() { return acaoAlvo; }
    public int getMetaQuantidade() { return metaQuantidade; } public int getRecompensaXp() { return recompensaXp; }
}
