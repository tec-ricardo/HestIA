package br.com.hestia.notificacao.dto;

import java.time.Instant;
import java.util.UUID;

public record NotificacaoResponse(
        UUID id,
        Long empresaId,
        String origem,
        String tipo,
        Double valor,
        String mensagem,
        String idempotencyKey,
        String nivel,
        String status,
        Instant createdAt,
        String correlationId
) {
}
