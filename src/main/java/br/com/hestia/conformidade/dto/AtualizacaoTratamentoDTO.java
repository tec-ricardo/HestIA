package br.com.hestia.conformidade.dto;

import br.com.hestia.conformidade.model.StatusTratamento;
import jakarta.validation.constraints.*;

public record AtualizacaoTratamentoDTO(
        @Positive Long responsavelId,
        @Size(max = 1000) String acaoCorretiva,
        @NotNull StatusTratamento status,
        @NotNull @PositiveOrZero Long versaoRegistro) {}
