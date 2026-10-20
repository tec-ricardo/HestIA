package br.com.hestia.indicadores.dto;

import java.math.BigDecimal;

public record FatorMaturidadeDTO(
        String codigo,
        String nome,
        BigDecimal peso
) {
}