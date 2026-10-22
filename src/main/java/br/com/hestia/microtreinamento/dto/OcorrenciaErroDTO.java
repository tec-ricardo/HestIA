package br.com.hestia.microtreinamento.dto;

import br.com.hestia.microtreinamento.model.TipoErro;

import java.time.LocalDateTime;

public record OcorrenciaErroDTO(
        String origemId,
        Long empresaId,
        Long usuarioId,
        TipoErro tipoErro,
        LocalDateTime dataOcorrencia
) {
    public OcorrenciaErroDTO {
        if (origemId == null || origemId.isBlank()
                || empresaId == null || empresaId <= 0
                || usuarioId == null || usuarioId <= 0
                || tipoErro == null || dataOcorrencia == null) {
            throw new IllegalArgumentException("Ocorrência inválida.");
        }
    }
}