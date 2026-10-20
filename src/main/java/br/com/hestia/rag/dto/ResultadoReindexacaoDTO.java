package br.com.hestia.rag.dto;

public record ResultadoReindexacaoDTO(
        Long respostaId,
        Status status
) {
    public enum Status {
        INDEXADA,
        INDISPONIVEL,
        CONFLITO,
        FALHA
    }
}