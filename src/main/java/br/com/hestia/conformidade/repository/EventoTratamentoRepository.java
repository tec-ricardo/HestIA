package br.com.hestia.conformidade.repository;

import br.com.hestia.conformidade.model.EventoTratamento;
import org.springframework.data.domain.*;
import org.springframework.data.repository.Repository;

public interface EventoTratamentoRepository extends Repository<EventoTratamento, Long> {
    EventoTratamento save(EventoTratamento evento);
    Page<EventoTratamento> findByTratamento_Id(Long tratamentoId, Pageable pagina);
}
