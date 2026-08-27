package br.com.hestia.politica.repository;

import br.com.hestia.politica.model.PoliticaUso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PoliticaUsoRepository extends JpaRepository<PoliticaUso, Long> {

    List<PoliticaUso> findByEmpresaId(Long empresaId);

    List<PoliticaUso> findByEmpresaIdAndAtivaTrue(Long empresaId);
}