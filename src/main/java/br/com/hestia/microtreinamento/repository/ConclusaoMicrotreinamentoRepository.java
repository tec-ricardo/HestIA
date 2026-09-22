package br.com.hestia.microtreinamento.repository;

import br.com.hestia.microtreinamento.model.ConclusaoMicrotreinamento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ConclusaoMicrotreinamentoRepository
        extends JpaRepository<ConclusaoMicrotreinamento, Long> {

    Optional<ConclusaoMicrotreinamento>
    findByUsuarioIdAndMicrotreinamentoId(
            Long usuarioId,
            Long microtreinamentoId
    );
}