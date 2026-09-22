package br.com.hestia.configuracao.repository;

import br.com.hestia.configuracao.model.ConfiguracaoEmpresa;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ConfiguracaoEmpresaRepository extends JpaRepository<ConfiguracaoEmpresa, Long> {
    Optional<ConfiguracaoEmpresa> findByEmpresaId(Long empresaId);
}
