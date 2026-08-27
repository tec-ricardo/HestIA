package br.com.hestia.empresa.model;

import jakarta.persistence.*;

@Entity
@Table(name = "empresas")
public class Empresa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    private String configuracoesGerais;

    private String politicas;

    private Double orcamento;

    public Empresa() {
    }

    public Empresa(Long id, String nome, String configuracoesGerais, String politicas, Double orcamento) {
        this.id = id;
        this.nome = nome;
        this.configuracoesGerais = configuracoesGerais;
        this.politicas = politicas;
        this.orcamento = orcamento;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getConfiguracoesGerais() {
        return configuracoesGerais;
    }

    public void setConfiguracoesGerais(String configuracoesGerais) {
        this.configuracoesGerais = configuracoesGerais;
    }

    public String getPoliticas() {
        return politicas;
    }

    public void setPoliticas(String politicas) {
        this.politicas = politicas;
    }

    public Double getOrcamento() {
        return orcamento;
    }

    public void setOrcamento(Double orcamento) {
        this.orcamento = orcamento;
    }
}