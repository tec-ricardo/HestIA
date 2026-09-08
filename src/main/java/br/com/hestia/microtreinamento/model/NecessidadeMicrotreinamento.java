package br.com.hestia.microtreinamento.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "necessidades_microtreinamento")
public class NecessidadeMicrotreinamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long usuarioId;

    @Enumerated(EnumType.STRING)
    private TipoErro tipoErro;

    @Enumerated(EnumType.STRING)
    private TemaTreinamento temaTreinamento;

    private LocalDateTime dataIdentificacao;

    private boolean necessario;

    public NecessidadeMicrotreinamento() {
    }

    public NecessidadeMicrotreinamento(
            Long usuarioId,
            TipoErro tipoErro,
            TemaTreinamento temaTreinamento,
            LocalDateTime dataIdentificacao,
            boolean necessario) {
        this.usuarioId = usuarioId;
        this.tipoErro = tipoErro;
        this.temaTreinamento = temaTreinamento;
        this.dataIdentificacao = dataIdentificacao;
        this.necessario = necessario;
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

    public TipoErro getTipoErro() {
        return tipoErro;
    }

    public void setTipoErro(TipoErro tipoErro) {
        this.tipoErro = tipoErro;
    }

    public TemaTreinamento getTemaTreinamento() {
        return temaTreinamento;
    }

    public void setTemaTreinamento(TemaTreinamento temaTreinamento) {
        this.temaTreinamento = temaTreinamento;
    }

    public LocalDateTime getDataIdentificacao() {
        return dataIdentificacao;
    }

    public void setDataIdentificacao(LocalDateTime dataIdentificacao) {
        this.dataIdentificacao = dataIdentificacao;
    }

    public boolean isNecessario() {
        return necessario;
    }

    public void setNecessario(boolean necessario) {
        this.necessario = necessario;
    }
}