package br.com.hestia.empresa.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

public class EmpresaDTO {

    @NotBlank(message = "O nome da empresa é obrigatório")
    private String nome;

    private String configuracoesGerais;

    private String politicas;

    @PositiveOrZero(message = "O orçamento não pode ser negativo")
    private Double orcamento;

    public EmpresaDTO() {
    }

    public EmpresaDTO(String nome, String configuracoesGerais,
                      String politicas, Double orcamento) {
        this.nome = nome;
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