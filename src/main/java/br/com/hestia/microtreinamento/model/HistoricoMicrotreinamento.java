package br.com.hestia.microtreinamento.model;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "historicos_microtreinamento")
public class HistoricoMicrotreinamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long usuarioId;

    @OneToMany(
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @JoinColumn(name = "historico_id")
    private List<ConclusaoMicrotreinamento> conclusoes = new ArrayList<>();

    public HistoricoMicrotreinamento() {
    }

    public HistoricoMicrotreinamento(Long usuarioId) {
        this.usuarioId = usuarioId;
    }

    public Long getId() {
        return id;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(Long usuarioId) {
        this.usuarioId = usuarioId;
    }

    public List<ConclusaoMicrotreinamento> getConclusoes() {
        return conclusoes;
    }

    public void setConclusoes(List<ConclusaoMicrotreinamento> conclusoes) {
        this.conclusoes = conclusoes;
    }

    public void adicionarConclusao(
            ConclusaoMicrotreinamento conclusao
    ) {
        this.conclusoes.add(conclusao);
    }
}