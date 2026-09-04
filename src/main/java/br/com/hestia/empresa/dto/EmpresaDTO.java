package br.com.hestia.empresa.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public class EmpresaDTO {

    @NotBlank(message = "O nome da empresa é obrigatório")
    @Size(min = 2, max = 150)
    private String nome;

    @NotBlank(message = "O CNPJ é obrigatório")
    @Pattern(regexp = "(?:\\d{14}|\\d{2}\\.\\d{3}\\.\\d{3}/\\d{4}-\\d{2})",
            message = "O CNPJ deve conter 14 dígitos")
    private String cnpj;

    private String configuracoesGerais;

    @PositiveOrZero(message = "O orçamento não pode ser negativo")
    private Double orcamento;

    public EmpresaDTO() {
    }

    public EmpresaDTO(String nome, String cnpj, String configuracoesGerais, Double orcamento) {
        this.nome = nome;
        this.cnpj = cnpj;
        this.configuracoesGerais = configuracoesGerais;
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

    public Double getOrcamento() {
        return orcamento;
    }

    public void setOrcamento(Double orcamento) {
        this.orcamento = orcamento;
    }
}
