package br.com.hestia.rag.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AtualizacaoConteudoDTO(
        @NotBlank String promptOriginal,
        @NotBlank String respostaGerada,
        @NotBlank @Size(max = 500) String origem,
        @NotBlank @Size(max = 50) String tipoConteudo
) {
}