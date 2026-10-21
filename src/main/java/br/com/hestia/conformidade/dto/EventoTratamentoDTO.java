package br.com.hestia.conformidade.dto;

import br.com.hestia.conformidade.model.*;
import java.time.LocalDateTime;

public record EventoTratamentoDTO(Long id, long sequencia, TipoEventoTratamento tipo,
        Long autorId, LocalDateTime registradoEm, StatusTratamento statusTratamento,
        Long responsavelId, String acaoCorretiva, StatusConformidade resultadoAnterior,
        StatusConformidade resultadoVigente, String justificativa) {
    public static EventoTratamentoDTO from(EventoTratamento e) {
        return new EventoTratamentoDTO(e.getId(), e.getSequencia(), e.getTipo(),
                e.getAutor() == null ? null : e.getAutor().getId(), e.getRegistradoEm(),
                e.getStatusTratamento(), e.getResponsavelId(), e.getAcaoCorretiva(),
                e.getResultadoAnterior(), e.getResultadoVigente(), e.getJustificativa());
    }
}
