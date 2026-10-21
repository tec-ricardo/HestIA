package br.com.hestia.politica.repository;

import br.com.hestia.politica.model.PoliticaUso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PoliticaUsoRepository extends JpaRepository<PoliticaUso, Long> {


    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select p from PoliticaUso p where p.id = :id")
    java.util.Optional<PoliticaUso> buscarBloqueada(@org.springframework.data.repository.query.Param("id") Long id);

    List<PoliticaUso> findByEmpresaId(Long empresaId);

    List<PoliticaUso> findByEmpresaIdAndAtivaTrue(Long empresaId);
}