package br.com.hestia.configuracao.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

public record ConfiguracaoEmpresaDTO(boolean registrarUsosAutomaticamente,
                                     boolean bloquearFerramentasNaoAprovadas,
                                     @PositiveOrZero BigDecimal limiteCustoMensal,
                                     @Min(1) int diasRetencaoAuditoria) {}
