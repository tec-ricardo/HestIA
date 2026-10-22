package br.com.hestia.registro.repository;

import br.com.hestia.registro.model.RegistroUsoIA;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface RegistroUsoIARepository
        extends JpaRepository<RegistroUsoIA, Long>,
        JpaSpecificationExecutor<RegistroUsoIA> {

    long countByUsuarioEmpresaId(Long empresaId);
    java.util.Optional<RegistroUsoIA> findByUsuarioIdAndOrigemAndEventoOrigem(Long usuarioId,br.com.hestia.registro.model.OrigemRegistro origem,String evento);
}
