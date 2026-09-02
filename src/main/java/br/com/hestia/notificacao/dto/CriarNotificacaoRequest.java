package br.com.hestia.notificacao.dto;

public record CriarNotificacaoRequest(
        Long empresaId,
        String origem,
        String tipo,
        Double valor,
        String mensagem,
        String idempotencyKey
) {
}
