package br.com.hestia.conformidade.dto;

import br.com.hestia.conformidade.model.*;
import java.time.LocalDateTime;

public record TratamentoResumoDTO(Long id, Long avaliacaoId, Long empresaId, Long departamentoId,
        StatusTratamento status, StatusConformidade resultadoOriginal,
        Long criadoPorId, LocalDateTime criadoEm, Long responsavelId, String acaoCorretiva) {
    public static TratamentoResumoDTO from(TratamentoNaoConformidade t) {
        return new TratamentoResumoDTO(t.getId(), t.getAvaliacao().getId(),
                t.getEmpresa().getId(), t.getDepartamento().getId(), t.getStatus(),
                t.getAvaliacao().getStatus(), t.getCriadoPor().getId(), t.getCriadoEm(),
                t.getResponsavel() == null ? null : t.getResponsavel().getId(), t.getAcaoCorretiva());
    }
}
