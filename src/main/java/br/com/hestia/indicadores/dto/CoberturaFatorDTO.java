package br.com.hestia.indicadores.dto;

import java.math.BigDecimal;

public record CoberturaFatorDTO(
        String codigo,
        String nome,
        BigDecimal peso,
        BigDecimal nota,
        boolean disponivel,
        String motivoAusencia
) {
}