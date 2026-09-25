package br.com.hestia.reutilizavel.dto;

import java.math.BigDecimal;

public record SustentabilidadeDTO(
        Long empresaId,
        long reutilizacoes,
        long tokensEconomizados,
        BigDecimal custoEstimadoEvitado,
        double percentualReutilizacao
) {
}