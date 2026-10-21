package br.com.hestia.indicadores.dto;

public record ConsultaCoberturaDTO(
        Long empresaId,
        String situacao,
        CoberturaMaturidadeDTO cobertura
) {
}