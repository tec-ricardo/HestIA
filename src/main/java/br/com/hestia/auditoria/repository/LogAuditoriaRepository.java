package br.com.hestia.auditoria.repository;

import br.com.hestia.auditoria.model.LogAuditoria;
import br.com.hestia.auditoria.model.TipoAcaoAuditoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LogAuditoriaRepository
        extends JpaRepository<LogAuditoria, Long> {

    List<LogAuditoria> findAllByOrderByDataHoraDesc();

    List<LogAuditoria> findByUsuarioIdOrderByDataHoraDesc(
            Long usuarioId
    );

    List<LogAuditoria> findByEntidadeIgnoreCaseOrderByDataHoraDesc(
            String entidade
    );

    List<LogAuditoria> findByAcaoOrderByDataHoraDesc(
            TipoAcaoAuditoria acao
    );
}