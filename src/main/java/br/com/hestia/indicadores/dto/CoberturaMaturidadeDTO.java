package br.com.hestia.indicadores.dto;

import java.math.BigDecimal;
import java.util.List;

public record CoberturaMaturidadeDTO(
        String versaoRegra,
        PeriodoReferenciaDTO periodoReferencia,
        int totalFatores,
        int fatoresDisponiveis,
        BigDecimal coberturaPercentual,
        BigDecimal score,
        List<CoberturaFatorDTO> fatores
) {
    public CoberturaMaturidadeDTO {
        fatores = List.copyOf(fatores);
    }
}