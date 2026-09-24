package br.com.hestia.auditoria.repository;

import br.com.hestia.auditoria.model.LogAuditoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface LogAuditoriaRepository extends JpaRepository<LogAuditoria, Long>, JpaSpecificationExecutor<LogAuditoria> {}
