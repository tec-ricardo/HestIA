package br.com.hestia.reutilizacao.model;

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

    /*
     * Estrutura temporária.
     * Será adaptada para vetor quando o banco vetorial for implementado.
     */
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

        if (possuiDadosSensiveis == null) {
            possuiDadosSensiveis = false;
        }

        if (reutilizavel == null) {
            reutilizavel = true;
        }
    }

    public void registrarReutilizacao() {
        this.quantidadeReutilizacoes++;
    }

    // getters e setters
}