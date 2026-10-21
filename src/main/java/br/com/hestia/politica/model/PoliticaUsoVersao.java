package br.com.hestia.politica.model;

import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;
import java.time.LocalDateTime;

@Entity
@Immutable
@Table(name = "politica_uso_versoes", uniqueConstraints =
        @UniqueConstraint(columnNames = {"politica_uso_id", "revisao"}))
public class PoliticaUsoVersao {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "politica_uso_id", nullable = false, updatable = false)
    private PoliticaUso politicaUso;
    @Column(nullable = false, updatable = false)
    private Long empresaId;
    @Column(nullable = false, updatable = false)
    private int revisao;
    @Column(nullable = false, length = 150, updatable = false)
    private String titulo;
    @Column(nullable = false, length = 500, updatable = false)
    private String descricao;
    @Column(nullable = false, columnDefinition = "TEXT", updatable = false)
    private String conteudo;
    @Column(nullable = false, length = 20, updatable = false)
    private String versao;
    @Column(nullable = false, updatable = false)
    private boolean ativa;
    @Column(nullable = false, updatable = false)
    private boolean excluida;
    @Column(nullable = false, length = 30, updatable = false)
    private String origem;
    @Column(nullable = false, updatable = false)
    private LocalDateTime registradaEm;
    @Column(updatable = false)
    private Long autorId;

    protected PoliticaUsoVersao() {}
    public PoliticaUsoVersao(PoliticaUso politica, Long autorId, String origem) {
        this.politicaUso = politica;
        this.empresaId = politica.getEmpresa().getId();
        this.revisao = politica.getRevisao();
        this.titulo = politica.getTitulo();
        this.descricao = politica.getDescricao();
        this.conteudo = politica.getConteudo();
        this.versao = politica.getVersao();
        this.ativa = Boolean.TRUE.equals(politica.getAtiva());
        this.excluida = politica.isExcluida();
        this.origem = origem;
        this.autorId = autorId;
        this.registradaEm = LocalDateTime.now();
    }
    public Long getId() { return id; }
    public Long getPoliticaUsoId() { return politicaUso.getId(); }
    public Long getEmpresaId() { return empresaId; }
    public int getRevisao() { return revisao; }
    public String getTitulo() { return titulo; }
    public String getDescricao() { return descricao; }
    public String getConteudo() { return conteudo; }
    public String getVersao() { return versao; }
    public boolean isAtiva() { return ativa; }
    public boolean isExcluida() { return excluida; }
    public String getOrigem() { return origem; }
    public LocalDateTime getRegistradaEm() { return registradaEm; }
    public Long getAutorId() { return autorId; }
}
