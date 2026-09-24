package br.com.hestia.auditoria.dto;

import br.com.hestia.auditoria.model.LogAuditoria;
import br.com.hestia.auditoria.model.ResultadoAuditoria;
import br.com.hestia.auditoria.model.TipoAcaoAuditoria;

import java.time.LocalDateTime;

public record LogAuditoriaResponseDTO(
        Long id,
        Long usuarioId,
        TipoAcaoAuditoria acao,
        String entidade,
        Long entidadeId,
        String descricao,
        LocalDateTime dataHora,
        ResultadoAuditoria resultado,
        String dadosAnteriores,
        String dadosNovos
) {

    public static LogAuditoriaResponseDTO from(LogAuditoria log) {
        return new LogAuditoriaResponseDTO(
                log.getId(),
                log.getUsuarioId(),
                log.getAcao(),
                log.getEntidade(),
                log.getEntidadeId(),
                log.getDescricao(),
                log.getDataHora(),
                log.getResultado(),
                log.getDadosAnteriores(),
                log.getDadosNovos()
        );
    }
}