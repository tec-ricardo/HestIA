package br.com.hestia.reutilizavel.repository;

import br.com.hestia.reutilizavel.model.RespostaReutilizavel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;
import br.com.hestia.usuario.model.PerfilUsuario;

import java.util.List;

@Repository
public interface RespostaReutilizavelRepository
        extends JpaRepository<RespostaReutilizavel, Long> {

    @Query("""
            select r from RespostaReutilizavel r
            where r.empresaId = :empresaId
              and :perfil member of r.perfisPermitidos
              and r.reutilizavel = true and r.possuiDadosSensiveis = false
              and (:id is null or r.id = :id)
              and (:prompt is null or lower(r.promptOriginal) like
                   lower(concat('%', :prompt, '%')) escape '!')
              and (:exigirEmbedding = false or (r.embedding is not null and trim(r.embedding) <> ''))
            order by r.dataCriacao desc, r.id desc
            """)
    List<RespostaReutilizavel> recuperarAutorizadas(
            @Param("empresaId") Long empresaId, @Param("perfil") PerfilUsuario perfil,
            @Param("id") Long id, @Param("prompt") String prompt,
            @Param("exigirEmbedding") boolean exigirEmbedding, Pageable pageable);

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