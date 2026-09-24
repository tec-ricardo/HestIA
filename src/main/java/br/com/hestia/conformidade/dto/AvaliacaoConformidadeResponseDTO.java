package br.com.hestia.conformidade.dto;

import br.com.hestia.conformidade.model.AvaliacaoConformidade;
import br.com.hestia.conformidade.model.CriterioConformidade;
import br.com.hestia.conformidade.model.StatusConformidade;

import java.time.LocalDateTime;
import java.util.Set;

public record AvaliacaoConformidadeResponseDTO(
        Long id,
        Long registroUsoId,
        Long politicaUsoId,
        StatusConformidade status,
        Set<CriterioConformidade> criteriosAtendidos,
        String justificativa,
        LocalDateTime dataAvaliacao
) {
    public static AvaliacaoConformidadeResponseDTO from(AvaliacaoConformidade avaliacao) {
        return new AvaliacaoConformidadeResponseDTO(
                avaliacao.getId(), avaliacao.getRegistroUso().getId(),
                avaliacao.getPoliticaUso().getId(), avaliacao.getStatus(),
                avaliacao.getCriteriosAtendidos(), avaliacao.getJustificativa(),
                avaliacao.getDataAvaliacao());
    }
}
