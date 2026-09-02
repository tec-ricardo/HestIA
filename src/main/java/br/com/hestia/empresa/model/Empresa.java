package br.com.hestia.empresa.model;

import jakarta.persistence.*;

@Entity
@Table(name = "empresas")
public class Empresa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String nome;

    @Column(nullable = false, unique = true, length = 14)
    private String cnpj;

    private String configuracoesGerais;

    private Double orcamento;

    public Empresa() {
    }

    public Empresa(Long id, String nome, String cnpj, String configuracoesGerais, Double orcamento) {
        this.id = id;
        this.nome = nome;
        this.cnpj = cnpj;
        this.configuracoesGerais = configuracoesGerais;
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

    public String getCnpj() {
        return cnpj;
    }

    public void setCnpj(String cnpj) {
        this.cnpj = cnpj;
    }

    public String getConfiguracoesGerais() {
        return configuracoesGerais;
    }

    public void setConfiguracoesGerais(String configuracoesGerais) {
        this.configuracoesGerais = configuracoesGerais;
    }

    public Double getOrcamento() {
        return orcamento;
    }

    public void setOrcamento(Double orcamento) {
        this.orcamento = orcamento;
    }
}
