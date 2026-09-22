package br.com.hestia.reutilizavel.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public class RespostaReutilizavelDTO {

    private Long usuarioId;

    @NotNull(message = "A empresa é obrigatória")
    private Long empresaId;

    @NotBlank(message = "O prompt original é obrigatório")
    private String promptOriginal;

    @NotBlank(message = "A resposta gerada é obrigatória")
    private String respostaGerada;

    private String embedding;

    private String categoria;

    private String modeloIA;

    @PositiveOrZero(message = "A quantidade de tokens de entrada não pode ser negativa")
    private Integer tokensEntrada;

    @PositiveOrZero(message = "A quantidade de tokens de saída não pode ser negativa")
    private Integer tokensSaida;

    private Boolean possuiDadosSensiveis;

    private Boolean reutilizavel;

    public RespostaReutilizavelDTO() {
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(Long usuarioId) {
        this.usuarioId = usuarioId;
    }

    public Long getEmpresaId() {
        return empresaId;
    }

    public void setEmpresaId(Long empresaId) {
        this.empresaId = empresaId;
    }

    public String getPromptOriginal() {
        return promptOriginal;
    }

    public void setPromptOriginal(String promptOriginal) {
        this.promptOriginal = promptOriginal;
    }

    public String getRespostaGerada() {
        return respostaGerada;
    }

    public void setRespostaGerada(String respostaGerada) {
        this.respostaGerada = respostaGerada;
    }

    public String getEmbedding() {
        return embedding;
    }

    public void setEmbedding(String embedding) {
        this.embedding = embedding;
    }

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    public String getModeloIA() {
        return modeloIA;
    }

    public void setModeloIA(String modeloIA) {
        this.modeloIA = modeloIA;
    }

    public Integer getTokensEntrada() {
        return tokensEntrada;
    }

    public void setTokensEntrada(Integer tokensEntrada) {
        this.tokensEntrada = tokensEntrada;
    }

    public Integer getTokensSaida() {
        return tokensSaida;
    }

    public void setTokensSaida(Integer tokensSaida) {
        this.tokensSaida = tokensSaida;
    }

    public Boolean getPossuiDadosSensiveis() {
        return possuiDadosSensiveis;
    }

    public void setPossuiDadosSensiveis(Boolean possuiDadosSensiveis) {
        this.possuiDadosSensiveis = possuiDadosSensiveis;
    }

    public Boolean getReutilizavel() {
        return reutilizavel;
    }

    public void setReutilizavel(Boolean reutilizavel) {
        this.reutilizavel = reutilizavel;
    }
}