package br.com.hestia.conformidade.dto;

import br.com.hestia.conformidade.model.CriterioConformidade;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record AvaliacaoConformidadeDTO(
        @NotNull Long registroUsoId,
        @NotNull Long politicaUsoId,
        @NotNull Set<CriterioConformidade> criteriosConfirmados,
        @Size(max = 1000) String justificativa
) {
}
