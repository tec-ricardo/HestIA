package br.com.hestia.microtreinamento.dto;

import br.com.hestia.microtreinamento.model.TemaTreinamento;
import br.com.hestia.microtreinamento.model.TipoErro;

import java.time.LocalDateTime;

public class NecessidadeMicrotreinamentoDTO {

    private Long id;
    private Long usuarioId;
    private TipoErro tipoErro;
    private TemaTreinamento temaTreinamento;
    private LocalDateTime dataIdentificacao;
    private boolean necessario;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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