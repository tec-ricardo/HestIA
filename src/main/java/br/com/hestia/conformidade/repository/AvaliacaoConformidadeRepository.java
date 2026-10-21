package br.com.hestia.conformidade.repository;

import br.com.hestia.conformidade.model.AvaliacaoConformidade;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface AvaliacaoConformidadeRepository extends JpaRepository<AvaliacaoConformidade, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from AvaliacaoConformidade a where a.id = :id")
    Optional<AvaliacaoConformidade> buscarBloqueada(@Param("id") Long id);

    boolean existsByRegistroUsoId(Long registroUsoId);
}
