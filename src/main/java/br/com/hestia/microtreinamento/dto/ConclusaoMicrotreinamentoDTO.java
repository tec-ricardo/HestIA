package br.com.hestia.microtreinamento.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class ConclusaoMicrotreinamentoDTO {

    @NotNull(message = "O usuário é obrigatório.")
    @Positive(message = "O ID do usuário deve ser positivo.")
    private Long usuarioId;

    @NotNull(message = "O microtreinamento é obrigatório.")
    @Positive(message = "O ID do microtreinamento deve ser positivo.")
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