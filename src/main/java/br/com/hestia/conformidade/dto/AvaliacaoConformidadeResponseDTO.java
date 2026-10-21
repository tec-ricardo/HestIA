package br.com.hestia.conformidade.dto;

import br.com.hestia.conformidade.model.*;
import java.time.LocalDateTime;
import java.util.Set;

public record AvaliacaoConformidadeResponseDTO(
        Long id, Long registroUsoId, Long politicaUsoId, StatusConformidade status,
        Set<CriterioConformidade> criteriosAtendidos, String justificativa, String motivo,
        String regraAplicada, String limitacoes, LocalDateTime dataAvaliacao,
        Long politicaVersaoId, Integer politicaRevisao, String politicaVersao,
        String regraCodigo, String regraVersao, boolean fundamentosPreservados) {
    public static AvaliacaoConformidadeResponseDTO from(AvaliacaoConformidade a) {
        var v = a.getPoliticaVersao();
        return new AvaliacaoConformidadeResponseDTO(
                a.getId(), a.getRegistroUso().getId(), a.getPoliticaUso().getId(),
                a.getStatus(), a.getCriteriosAtendidos(), a.getJustificativa(),
                a.getMotivoPreservado(), a.getRegraPreservada(),
                v == null ? "Avaliação legada sem versão histórica e fundamentos preservados. "
                        + "Não é possível comprovar a política original." : a.getLimitacoesPreservadas(),
                a.getDataAvaliacao(), v == null ? null : v.getId(),
                v == null ? null : v.getRevisao(), v == null ? null : v.getVersao(),
                a.getRegraCodigo(), a.getRegraVersao(), v != null);
    }
}
