package br.com.hestia.microtreinamento.dto;

public class ConclusaoMicrotreinamentoDTO {

    private Long usuarioId;
    private Long microtreinamentoId;

    public ConclusaoMicrotreinamentoDTO() {
    }

    public ConclusaoMicrotreinamentoDTO(
            Long usuarioId,
            Long microtreinamentoId
    ) {
        this.usuarioId = usuarioId;
        this.microtreinamentoId = microtreinamentoId;
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
}