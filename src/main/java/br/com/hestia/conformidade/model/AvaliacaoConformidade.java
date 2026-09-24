package br.com.hestia.conformidade.model;

import br.com.hestia.politica.model.PoliticaUso;
import br.com.hestia.registro.model.RegistroUsoIA;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Set;

@Entity
@Table(name = "avaliacoes_conformidade")
public class AvaliacaoConformidade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "registro_uso_id", nullable = false, unique = true)
    private RegistroUsoIA registroUso;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "politica_uso_id", nullable = false)
    private PoliticaUso politicaUso;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StatusConformidade status;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "avaliacao_criterios_atendidos",
            joinColumns = @JoinColumn(name = "avaliacao_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "criterio", nullable = false, length = 40)
    private Set<CriterioConformidade> criteriosAtendidos = EnumSet.noneOf(CriterioConformidade.class);

    @Column(length = 1000)
    private String justificativa;

    @Column(name = "data_avaliacao", nullable = false)
    private LocalDateTime dataAvaliacao;

    protected AvaliacaoConformidade() {
    }

    public AvaliacaoConformidade(RegistroUsoIA registroUso, PoliticaUso politicaUso,
                                 StatusConformidade status,
                                 Set<CriterioConformidade> criteriosAtendidos,
                                 String justificativa) {
        this.registroUso = registroUso;
        this.politicaUso = politicaUso;
        this.status = status;
        this.criteriosAtendidos = criteriosAtendidos.isEmpty()
                ? EnumSet.noneOf(CriterioConformidade.class)
                : EnumSet.copyOf(criteriosAtendidos);
        this.justificativa = justificativa;
    }

    @PrePersist
    void prePersist() {
        if (dataAvaliacao == null) dataAvaliacao = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public RegistroUsoIA getRegistroUso() { return registroUso; }
    public PoliticaUso getPoliticaUso() { return politicaUso; }
    public StatusConformidade getStatus() { return status; }
    public Set<CriterioConformidade> getCriteriosAtendidos() { return Set.copyOf(criteriosAtendidos); }
    public String getJustificativa() { return justificativa; }
    public LocalDateTime getDataAvaliacao() { return dataAvaliacao; }
}
