package br.com.hestia.reutilizacao.repository;

import br.com.hestia.reutilizacao.model.RespostaReutilizavel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RespostaReutilizavelRepository
        extends JpaRepository<RespostaReutilizavel, Long> {

    List<RespostaReutilizavel> findByEmpresaId(Long empresaId);

    List<RespostaReutilizavel> findByEmpresaIdAndReutilizavelTrue(Long empresaId);
}