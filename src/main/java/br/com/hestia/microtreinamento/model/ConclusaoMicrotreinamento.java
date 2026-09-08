package br.com.hestia.microtreinamento.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "conclusoes_microtreinamento")
public class ConclusaoMicrotreinamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long usuarioId;

    private Long microtreinamentoId;

    private LocalDateTime dataConclusao;

    private boolean concluido;

    public ConclusaoMicrotreinamento() {
    }

    public ConclusaoMicrotreinamento(
            Long usuarioId,
            Long microtreinamentoId,
            LocalDateTime dataConclusao,
            boolean concluido
    ) {
        this.usuarioId = usuarioId;
        this.microtreinamentoId = microtreinamentoId;
        this.dataConclusao = dataConclusao;
        this.concluido = concluido;
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

    public Long getMicrotreinamentoId() {
        return microtreinamentoId;
    }

    public void setMicrotreinamentoId(Long microtreinamentoId) {
        this.microtreinamentoId = microtreinamentoId;
    }

    public LocalDateTime getDataConclusao() {
        return dataConclusao;
    }

    public void setDataConclusao(LocalDateTime dataConclusao) {
        this.dataConclusao = dataConclusao;
    }

    public boolean isConcluido() {
        return concluido;
    }

    public void setConcluido(boolean concluido) {
        this.concluido = concluido;
    }
}