package br.com.hestia.autenticacao.repository;

import br.com.hestia.autenticacao.model.RecuperacaoSenha;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.Optional;

public interface RecuperacaoSenhaRepository extends JpaRepository<RecuperacaoSenha, Long> {
    Optional<RecuperacaoSenha> findByTokenHash(String tokenHash);

    @Modifying
    @Query("update RecuperacaoSenha r set r.utilizadaEm = :instante "
            + "where r.usuario.id = :usuarioId and r.utilizadaEm is null")
    int invalidarTokensAtivos(@Param("usuarioId") Long usuarioId, @Param("instante") LocalDateTime instante);
}