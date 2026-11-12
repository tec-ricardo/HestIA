package br.com.hestia.autenticacao.repository;

import br.com.hestia.autenticacao.model.SessaoUsuario;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface SessaoUsuarioRepository extends JpaRepository<SessaoUsuario, Long> {
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("update SessaoUsuario s set s.revogadaEm = :agora where s.usuario.id = :usuarioId and s.revogadaEm is null")
    int revogarTodas(@org.springframework.data.repository.query.Param("usuarioId") Long usuarioId,
        @org.springframework.data.repository.query.Param("agora") java.time.LocalDateTime agora);
    Optional<SessaoUsuario> findByTokenHash(String tokenHash);
}
