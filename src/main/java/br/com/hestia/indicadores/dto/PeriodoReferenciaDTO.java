package br.com.hestia.indicadores.dto;

import java.time.LocalDate;

public record PeriodoReferenciaDTO(
        LocalDate inicio,
        LocalDate fim
) {
}