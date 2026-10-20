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
        String motivo,
        String regraAplicada,
        String limitacoes,
        LocalDateTime dataAvaliacao
) {
    public static AvaliacaoConformidadeResponseDTO from(AvaliacaoConformidade avaliacao) {
        FundamentosConformidade fundamentos = FundamentosConformidade.from(
                avaliacao.getStatus(), avaliacao.getCriteriosAtendidos());

        return new AvaliacaoConformidadeResponseDTO(
                avaliacao.getId(), avaliacao.getRegistroUso().getId(),
                avaliacao.getPoliticaUso().getId(), avaliacao.getStatus(),
                avaliacao.getCriteriosAtendidos(), avaliacao.getJustificativa(),
                fundamentos.motivo(), fundamentos.regraAplicada(),
                fundamentos.limitacoes(), avaliacao.getDataAvaliacao());
    }
}