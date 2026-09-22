package br.com.hestia.auditoria.dto;

import br.com.hestia.auditoria.model.LogAuditoria;
import java.time.LocalDateTime;

public record LogAuditoriaResponseDTO(Long id, Long usuarioId, String usuarioNome, Long empresaId,
                                      String metodo, String recurso, String acao, Integer statusHttp,
                                      String enderecoIp, LocalDateTime dataHora) {
    public static LogAuditoriaResponseDTO from(LogAuditoria log) {
        return new LogAuditoriaResponseDTO(log.getId(),
                log.getUsuario() == null ? null : log.getUsuario().getId(),
                log.getUsuario() == null ? null : log.getUsuario().getNome(),
                log.getEmpresa() == null ? null : log.getEmpresa().getId(),
                log.getMetodo(), log.getRecurso(), log.getAcao(), log.getStatusHttp(),
                log.getEnderecoIp(), log.getDataHora());
    }
}
