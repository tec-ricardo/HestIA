
package br.com.hestia.reutilizavel.dto;

public record IndicadoresQualidadeDTO(
        Long empresaId,
        long totalRespostas,
        long respostasDisponiveis,
        long respostasSeguras,
        long respostasReutilizadas,
        long totalReutilizacoes,
        double percentualDisponivel
) {
}
