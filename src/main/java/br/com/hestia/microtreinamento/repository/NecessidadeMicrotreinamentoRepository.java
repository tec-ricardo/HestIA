package br.com.hestia.microtreinamento.repository;

import br.com.hestia.microtreinamento.model.NecessidadeMicrotreinamento;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NecessidadeMicrotreinamentoRepository
        extends JpaRepository<NecessidadeMicrotreinamento, Long> {
}