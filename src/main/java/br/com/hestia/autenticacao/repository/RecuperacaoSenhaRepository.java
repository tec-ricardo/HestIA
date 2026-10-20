package br.com.hestia.autenticacao.repository;

import br.com.hestia.autenticacao.model.RecuperacaoSenha;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface RecuperacaoSenhaRepository extends JpaRepository<RecuperacaoSenha, Long> {
    Optional<RecuperacaoSenha> findByTokenHash(String tokenHash);
}