package br.com.hestia.registro.model;

import br.com.hestia.ferramenta.model.FerramentaIA;
import br.com.hestia.usuario.model.Usuario;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "registros_uso_ia", indexes = {
        @Index(name = "idx_registro_uso_usuario", columnList = "usuario_id"),
        @Index(name = "idx_registro_uso_ferramenta", columnList = "ferramenta_id"),
        @Index(name = "idx_registro_uso_data_hora", columnList = "data_hora")
})
public class RegistroUsoIA {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ferramenta_id", nullable = false)
    private FerramentaIA ferramenta;

    @Column(name = "modelo_ia", nullable = false, length = 100)
    private String modeloIa;

    @Column(name = "data_hora", nullable = false)
    private LocalDateTime dataHora;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private FinalidadeUso finalidade;

    @Column(name = "tokens_entrada", nullable = false)
    private Integer tokensEntrada;

    @Column(name = "tokens_saida", nullable = false)
    private Integer tokensSaida;

    @Column(name = "tokens_total", nullable = false)
    private Integer tokensTotal;

    @Column(name = "custo_estimado", precision = 15, scale = 6)
    private BigDecimal custoEstimado;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_execucao", nullable = false, length = 30)
    private StatusExecucao statusExecucao;

    @Column(name = "tempo_resposta_ms")
    private Long tempoRespostaMs;

    protected RegistroUsoIA() {
    }

    public RegistroUsoIA(Usuario usuario, FerramentaIA ferramenta, String modeloIa,
                         LocalDateTime dataHora, FinalidadeUso finalidade,
                         Integer tokensEntrada, Integer tokensSaida,
                         BigDecimal custoEstimado, StatusExecucao statusExecucao,
                         Long tempoRespostaMs) {
        this.usuario = usuario;
        this.ferramenta = ferramenta;
        this.modeloIa = modeloIa;
        this.dataHora = dataHora;
        this.finalidade = finalidade;
        this.tokensEntrada = tokensEntrada;
        this.tokensSaida = tokensSaida;
        this.custoEstimado = custoEstimado;
        this.statusExecucao = statusExecucao;
        this.tempoRespostaMs = tempoRespostaMs;
        calcularTokensTotal();
    }

    @PrePersist
    void antesDePersistir() {
        if (dataHora == null) {
            dataHora = LocalDateTime.now();
        }
        calcularTokensTotal();
    }

    @PreUpdate
    void antesDeAtualizar() {
        calcularTokensTotal();
    }

    private void calcularTokensTotal() {
        tokensTotal = valorSeguro(tokensEntrada) + valorSeguro(tokensSaida);
    }

    private int valorSeguro(Integer valor) {
        return valor == null ? 0 : valor;
    }

    public Long getId() { return id; }
    public Usuario getUsuario() { return usuario; }
    public FerramentaIA getFerramenta() { return ferramenta; }
    public String getModeloIa() { return modeloIa; }
    public LocalDateTime getDataHora() { return dataHora; }
    public FinalidadeUso getFinalidade() { return finalidade; }
    public Integer getTokensEntrada() { return tokensEntrada; }
    public Integer getTokensSaida() { return tokensSaida; }
    public Integer getTokensTotal() { return tokensTotal; }
    public BigDecimal getCustoEstimado() { return custoEstimado; }
    public StatusExecucao getStatusExecucao() { return statusExecucao; }
    public Long getTempoRespostaMs() { return tempoRespostaMs; }
}
