package br.com.hestia.ferramenta.model;

import br.com.hestia.empresa.model.Empresa;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "ferramentas_ia",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_ferramenta_empresa_nome_fornecedor",
                columnNames = {"empresa_id", "nome", "fornecedor"}
        ),
        indexes = {
                @Index(name = "idx_ferramenta_empresa", columnList = "empresa_id"),
                @Index(name = "idx_ferramenta_status", columnList = "status")
        }
)
public class FerramentaIA {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String nome;

    @Column(nullable = false, length = 120)
    private String fornecedor;

    @Column(length = 500)
    private String descricao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private TipoFerramentaIA tipo;

    @Column(name = "finalidade_uso", nullable = false, length = 500)
    private String finalidadeUso;

    @Column(name = "url_acesso", length = 2048)
    private String urlAcesso;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StatusFerramentaIA status;

    @Enumerated(EnumType.STRING)
    @Column(name = "nivel_risco", nullable = false, length = 30)
    private NivelRiscoIA nivelRisco;

    @Column(name = "trata_dados_pessoais", nullable = false)
    private Boolean trataDadosPessoais;

    @Column(name = "data_cadastro", nullable = false, updatable = false)
    private LocalDateTime dataCadastro;

    @Column(name = "data_atualizacao", nullable = false)
    private LocalDateTime dataAtualizacao;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    protected FerramentaIA() {
    }

    public FerramentaIA(String nome, String fornecedor, String descricao,
                        TipoFerramentaIA tipo, String finalidadeUso,
                        String urlAcesso, Boolean trataDadosPessoais,
                        Empresa empresa) {
        this.nome = nome;
        this.fornecedor = fornecedor;
        this.descricao = descricao;
        this.tipo = tipo;
        this.finalidadeUso = finalidadeUso;
        this.urlAcesso = urlAcesso;
        this.trataDadosPessoais = trataDadosPessoais;
        this.empresa = empresa;
    }

    @PrePersist
    void prePersist() {
        var agora = LocalDateTime.now();
        dataCadastro = agora;
        dataAtualizacao = agora;
        if (status == null) status = StatusFerramentaIA.EM_ANALISE;
        if (nivelRisco == null) nivelRisco = NivelRiscoIA.NAO_AVALIADO;
    }

    @PreUpdate
    void preUpdate() {
        dataAtualizacao = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getNome() { return nome; }
    public String getFornecedor() { return fornecedor; }
    public String getDescricao() { return descricao; }
    public TipoFerramentaIA getTipo() { return tipo; }
    public String getFinalidadeUso() { return finalidadeUso; }
    public String getUrlAcesso() { return urlAcesso; }
    public StatusFerramentaIA getStatus() { return status; }
    public NivelRiscoIA getNivelRisco() { return nivelRisco; }
    public Boolean getTrataDadosPessoais() { return trataDadosPessoais; }
    public LocalDateTime getDataCadastro() { return dataCadastro; }
    public LocalDateTime getDataAtualizacao() { return dataAtualizacao; }
    public Empresa getEmpresa() { return empresa; }
}
