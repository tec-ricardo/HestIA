package br.com.hestia.indicadores.dto;

import java.math.BigDecimal;
import java.util.List;

public record ComposicaoMaturidadeDTO(
        int notaMinima,
        int notaMaxima,
        String formula,
        BigDecimal somaPesos,
        List<FatorMaturidadeDTO> fatores
) {
    public ComposicaoMaturidadeDTO {
        fatores = List.copyOf(fatores);
    }
}