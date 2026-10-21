package br.com.hestia.indicadores.dto;

import java.math.BigDecimal;
import java.util.List;

public record ConfiancaIndicadorDTO(
        String classificacao, String situacao, String versaoRegra,
        BigDecimal cobertura, BigDecimal completude, long amostra,
        long universoObservado, Integer amostraMinima, BigDecimal limiteCobertura,
        List<String> motivos) {
    public ConfiancaIndicadorDTO { motivos = List.copyOf(motivos); }
}
