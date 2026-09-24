package br.com.hestia.conformidade.repository;

import br.com.hestia.conformidade.model.AvaliacaoConformidade;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AvaliacaoConformidadeRepository extends JpaRepository<AvaliacaoConformidade, Long> {
    boolean existsByRegistroUsoId(Long registroUsoId);
}
