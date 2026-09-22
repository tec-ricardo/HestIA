package br.com.hestia.reutilizavel.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "respostas_reutilizaveis")
public class RespostaReutilizavel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id")
    private Long usuarioId;

    @Column(name = "empresa_id", nullable = false)
    private Long empresaId;

    @Column(name = "prompt_original", nullable = false, columnDefinition = "TEXT")
    private String promptOriginal;

    @Column(name = "resposta_gerada", nullable = false, columnDefinition = "TEXT")
    private String respostaGerada;

    @Column(name = "embedding", columnDefinition = "TEXT")
    private String embedding;

    @Column(name = "categoria")
    private String categoria;

    @Column(name = "modelo_ia")
    private String modeloIA;

    @Column(name = "tokens_entrada")
    private Integer tokensEntrada;

    @Column(name = "tokens_saida")
    private Integer tokensSaida;

    @Column(name = "quantidade_reutilizacoes", nullable = false)
    private Integer quantidadeReutilizacoes = 0;

    @Column(name = "tokens_economizados", nullable = false)
    private Long tokensEconomizados = 0L;

    @Column(name = "possui_dados_sensiveis", nullable = false)
    private Boolean possuiDadosSensiveis = false;

    @Column(name = "reutilizavel", nullable = false)
    private Boolean reutilizavel = true;

    @Column(name = "data_criacao", nullable = false)
    private LocalDateTime dataCriacao;

    public RespostaReutilizavel() {
    }

    @PrePersist
    public void prePersist() {
        if (dataCriacao == null) {
            dataCriacao = LocalDateTime.now();
        }

        if (quantidadeReutilizacoes == null) {
            quantidadeReutilizacoes = 0;
        }
        if (tokensEconomizados == null) {
            tokensEconomizados = 0L;
        }

        if (possuiDadosSensiveis == null) {
            possuiDadosSensiveis = false;
        }

        if (reutilizavel == null) {
            reutilizavel = true;
        }
    }

    public void registrarReutilizacao() {
        this.quantidadeReutilizacoes++;
        this.tokensEconomizados += valorSeguro(tokensEntrada) + valorSeguro(tokensSaida);
    }

    private long valorSeguro(Integer valor) { return valor == null ? 0L : valor.longValue(); }

    public Long getId() {
        return id;
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

    public Integer getQuantidadeReutilizacoes() {
        return quantidadeReutilizacoes;
    }

    public void setQuantidadeReutilizacoes(Integer quantidadeReutilizacoes) {
        this.quantidadeReutilizacoes = quantidadeReutilizacoes;
    }

    public Integer getChamadasEvitadas() { return quantidadeReutilizacoes; }
    public Long getTokensEconomizados() { return tokensEconomizados; }

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

    public LocalDateTime getDataCriacao() {
        return dataCriacao;
    }

    public void setDataCriacao(LocalDateTime dataCriacao) {
        this.dataCriacao = dataCriacao;
    }
}
