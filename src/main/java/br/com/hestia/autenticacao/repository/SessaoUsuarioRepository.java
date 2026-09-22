package br.com.hestia.autenticacao.repository;

import br.com.hestia.autenticacao.model.SessaoUsuario;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface SessaoUsuarioRepository extends JpaRepository<SessaoUsuario, Long> {
    Optional<SessaoUsuario> findByTokenHash(String tokenHash);
}
