package br.com.hestia.reutilizavel.dto;

import java.math.BigDecimal;

public record EconomiaReutilizacaoDTO(
        Long empresaId,
        long chamadasEvitadas,
        long tokensEconomizados,
        BigDecimal custoEstimadoEvitado
) {
}