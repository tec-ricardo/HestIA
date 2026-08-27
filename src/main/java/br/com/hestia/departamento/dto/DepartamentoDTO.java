package br.com.hestia.departamento.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class DepartamentoDTO {

    @NotBlank(message = "O nome do departamento é obrigatório")
    private String nome;

    private String responsavel;

    private String estruturaHierarquica;

    @NotNull(message = "A empresa é obrigatória")
    private Long empresaId;

    public DepartamentoDTO() {
    }

    public DepartamentoDTO(String nome, String responsavel,
                           String estruturaHierarquica, Long empresaId) {
        this.nome = nome;
        this.responsavel = responsavel;
        this.estruturaHierarquica = estruturaHierarquica;
        this.empresaId = empresaId;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getResponsavel() {
        return responsavel;
    }

    public void setResponsavel(String responsavel) {
        this.responsavel = responsavel;
    }

    public String getEstruturaHierarquica() {
        return estruturaHierarquica;
    }

    public void setEstruturaHierarquica(String estruturaHierarquica) {
        this.estruturaHierarquica = estruturaHierarquica;
    }

    public Long getEmpresaId() {
        return empresaId;
    }

    public void setEmpresaId(Long empresaId) {
        this.empresaId = empresaId;
    }
}