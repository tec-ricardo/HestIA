package br.com.hestia.trilha.model;
import jakarta.persistence.*;
import java.util.*;

@Entity @Table(name="trilhas")
public class Trilha {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false) private Long empresaId;
    @Column(nullable=false) private String titulo;
    @Column(nullable=false) private boolean exigeAprovacao;
    @ElementCollection @CollectionTable(name="trilha_etapas",joinColumns=@JoinColumn(name="trilha_id"))
    @OrderColumn(name="ordem") @Column(name="microtreinamento_id",nullable=false)
    private List<Long> etapas=new ArrayList<>();
    protected Trilha() {}
    public Trilha(Long empresaId,String titulo,List<Long> etapas) {
        this.empresaId=empresaId; this.titulo=titulo; this.etapas=new ArrayList<>(etapas);
    }
    public Long getId(){return id;}
    public Long getEmpresaId(){return empresaId;}
    public String getTitulo(){return titulo;}
    public List<Long> getEtapas(){return List.copyOf(etapas);}
    public boolean isExigeAprovacao(){return exigeAprovacao;}
    public void setExigeAprovacao(boolean valor){this.exigeAprovacao=valor;}
}
