package br.com.hestia.registro.dto;

import java.math.BigDecimal;

public record EficienciaUsoDTO(
        String departamento,
        long utilizacoes,
        double eficiencia,
        BigDecimal custoIA
) {
}