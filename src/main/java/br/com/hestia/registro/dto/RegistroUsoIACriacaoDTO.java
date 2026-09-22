package br.com.hestia.registro.dto;

import br.com.hestia.registro.model.FinalidadeUso;
import br.com.hestia.registro.model.StatusExecucao;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record RegistroUsoIACriacaoDTO(
        @NotNull Long usuarioId,
        @NotNull Long ferramentaId,
        @NotBlank @Size(max = 100) String modeloIa,
        LocalDateTime dataHora,
        @NotNull FinalidadeUso finalidade,
        @NotNull @PositiveOrZero Integer tokensEntrada,
        @NotNull @PositiveOrZero Integer tokensSaida,
        @PositiveOrZero BigDecimal custoEstimado,
        @NotNull StatusExecucao statusExecucao,
        @PositiveOrZero Long tempoRespostaMs
) {
}
