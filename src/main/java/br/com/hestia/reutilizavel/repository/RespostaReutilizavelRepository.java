package br.com.hestia.reutilizavel.repository;

import br.com.hestia.reutilizavel.model.RespostaReutilizavel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RespostaReutilizavelRepository
        extends JpaRepository<RespostaReutilizavel, Long> {

    List<RespostaReutilizavel> findByEmpresaId(Long empresaId);

    List<RespostaReutilizavel> findByEmpresaIdAndReutilizavelTrue(
            Long empresaId
    );

    List<RespostaReutilizavel>
    findTop10ByEmpresaIdAndReutilizavelTrueAndPromptOriginalContainingIgnoreCaseOrderByDataCriacaoDesc(
            Long empresaId,
            String prompt
    );

    List<RespostaReutilizavel>
    findByEmpresaIdAndReutilizavelTrueAndPossuiDadosSensiveisFalseAndEmbeddingIsNotNull(
            Long empresaId
    );
}