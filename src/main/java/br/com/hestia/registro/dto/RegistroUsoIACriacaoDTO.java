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
        @PositiveOrZero Long tempoRespostaMs,
        @jakarta.validation.Valid @Size(max=4) java.util.List<Evidencia> evidencias
) {
    public record Evidencia(@NotNull br.com.hestia.conformidade.model.CriterioConformidade criterio,
        @NotNull br.com.hestia.conformidade.service.DecisaoEvidencias.Estado estado,
        @NotBlank @Size(max=2000) String referencia){}
    public RegistroUsoIACriacaoDTO(Long usuarioId,Long ferramentaId,String modeloIa,LocalDateTime dataHora,FinalidadeUso finalidade,Integer tokensEntrada,Integer tokensSaida,BigDecimal custoEstimado,StatusExecucao statusExecucao,Long tempoRespostaMs){
        this(usuarioId,ferramentaId,modeloIa,dataHora,finalidade,tokensEntrada,tokensSaida,custoEstimado,statusExecucao,tempoRespostaMs,null);
    }
}
