package br.com.hestia.conformidade.model;

import br.com.hestia.usuario.model.Usuario;
import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;
import java.time.LocalDateTime;

@Entity
@Immutable
@Table(name = "historico_tratamentos")
public class EventoTratamento {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tratamento_id", nullable = false, updatable = false)
    private TratamentoNaoConformidade tratamento;
    @Column(nullable = false, updatable = false)
    private long sequencia;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30, updatable = false)
    private TipoEventoTratamento tipo;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "autor_id", updatable = false)
    private Usuario autor;
    @Column(nullable = false, updatable = false)
    private LocalDateTime registradoEm;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30, updatable = false)
    private StatusTratamento statusTratamento;
    @Column(updatable = false)
    private Long responsavelId;
    @Column(length = 1000, updatable = false)
    private String acaoCorretiva;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30, updatable = false)
    private StatusConformidade resultadoAnterior;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30, updatable = false)
    private StatusConformidade resultadoVigente;
    @Column(length = 1000, updatable = false)
    private String justificativa;

    protected EventoTratamento() {}

    public EventoTratamento(TratamentoNaoConformidade t, Usuario autor, TipoEventoTratamento tipo,
            StatusConformidade anterior, String justificativa) {
        if (autor == null || !t.getEmpresa().getId().equals(autor.getEmpresa().getId()))
            throw new IllegalArgumentException("Autor do histórico incompatível com a empresa.");
        this.tratamento = t;
        this.sequencia = t.getVersaoRegistro();
        this.tipo = tipo;
        this.autor = autor;
        this.registradoEm = LocalDateTime.now();
        this.statusTratamento = t.getStatus();
        this.responsavelId = t.getResponsavel() == null ? null : t.getResponsavel().getId();
        this.acaoCorretiva = t.getAcaoCorretiva();
        this.resultadoAnterior = anterior;
        this.resultadoVigente = t.getResultadoVigente();
        this.justificativa = justificativa;
    }
    public Long getId() { return id; }
    public long getSequencia() { return sequencia; }
    public TipoEventoTratamento getTipo() { return tipo; }
    public Usuario getAutor() { return autor; }
    public LocalDateTime getRegistradoEm() { return registradoEm; }
    public StatusTratamento getStatusTratamento() { return statusTratamento; }
    public Long getResponsavelId() { return responsavelId; }
    public String getAcaoCorretiva() { return acaoCorretiva; }
    public StatusConformidade getResultadoAnterior() { return resultadoAnterior; }
    public StatusConformidade getResultadoVigente() { return resultadoVigente; }
    public String getJustificativa() { return justificativa; }
}
