package br.com.hestia.conformidade.model;

import br.com.hestia.departamento.model.Departamento;
import br.com.hestia.empresa.model.Empresa;
import br.com.hestia.usuario.model.Usuario;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "tratamentos_nao_conformidade")
public class TratamentoNaoConformidade {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "avaliacao_id", nullable = false, unique = true, updatable = false)
    private AvaliacaoConformidade avaliacao;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "empresa_id", nullable = false, updatable = false)
    private Empresa empresa;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "departamento_id", nullable = false, updatable = false)
    private Departamento departamento;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "criado_por_id", nullable = false, updatable = false)
    private Usuario criadoPor;

    @Column(nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StatusTratamento status = StatusTratamento.ABERTA;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "responsavel_id")
    private Usuario responsavel;

    @Column(length = 1000)
    private String acaoCorretiva;

    @Version
    private long versaoRegistro;

    protected TratamentoNaoConformidade() {}

    public TratamentoNaoConformidade(AvaliacaoConformidade avaliacao, Usuario autor) {
        if (avaliacao.getStatus() != StatusConformidade.NAO_CONFORME)
            throw new IllegalArgumentException("Somente avaliações NAO_CONFORME podem originar tratamento.");
        var origem = avaliacao.getRegistroUso().getUsuario();
        if (origem.getDepartamento() == null || origem.getEmpresa() == null
                || !origem.getEmpresa().getId().equals(autor.getEmpresa().getId())
                || !origem.getEmpresa().getId().equals(avaliacao.getPoliticaUso().getEmpresa().getId())
                || !origem.getEmpresa().getId().equals(origem.getDepartamento().getEmpresa().getId()))
            throw new IllegalArgumentException("Vínculos da ocorrência incompatíveis com a empresa.");
        this.avaliacao = avaliacao;
        this.empresa = origem.getEmpresa();
        this.departamento = origem.getDepartamento();
        this.criadoPor = autor;
        this.criadoEm = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public AvaliacaoConformidade getAvaliacao() { return avaliacao; }
    public Empresa getEmpresa() { return empresa; }
    public Departamento getDepartamento() { return departamento; }
    public Usuario getCriadoPor() { return criadoPor; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
    public StatusTratamento getStatus() { return status; }
    public Usuario getResponsavel() { return responsavel; }
    public String getAcaoCorretiva() { return acaoCorretiva; }
}
