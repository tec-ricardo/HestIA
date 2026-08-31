package br.com.hestia.empresa.dto;

public class EmpresaDTO {

    private String nome;
    private String cnpj;
    private String configuracoesGerais;
    private String politicas;
    private Double orcamento;

    public EmpresaDTO() {
    }

    public EmpresaDTO(String nome, String cnpj, String configuracoesGerais, String politicas, Double orcamento) {
        this.nome = nome;
        this.cnpj = cnpj;
        this.configuracoesGerais = configuracoesGerais;
        this.politicas = politicas;
        this.orcamento = orcamento;
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