package br.com.hestia.microtreinamento.repository;

import br.com.hestia.microtreinamento.model.HistoricoMicrotreinamento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface HistoricoMicrotreinamentoRepository
        extends JpaRepository<HistoricoMicrotreinamento, Long> {

    Optional<HistoricoMicrotreinamento> findByUsuarioId(Long usuarioId);
}