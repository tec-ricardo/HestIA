package br.com.hestia.microtreinamento.repository;

import br.com.hestia.microtreinamento.model.ConclusaoMicrotreinamento;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConclusaoMicrotreinamentoRepository
        extends JpaRepository<ConclusaoMicrotreinamento, Long> {
}