package br.com.hestia.rag.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record BuscaSemelhanteDTO(
        @NotBlank String pergunta,
        @Min(1) @Max(20) Integer limite,
        @DecimalMin("0.0") @DecimalMax("1.0") Double similaridadeMinima
) {
}