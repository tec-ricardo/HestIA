package br.com.hestia.microtreinamento.service;

import br.com.hestia.microtreinamento.model.CriterioErroRecorrente;
import br.com.hestia.microtreinamento.model.NecessidadeMicrotreinamento;
import br.com.hestia.microtreinamento.model.TipoErro;
import br.com.hestia.microtreinamento.repository.NecessidadeMicrotreinamentoRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class NecessidadeMicrotreinamentoService {

    private final NecessidadeMicrotreinamentoRepository repository;

    public NecessidadeMicrotreinamentoService(
            NecessidadeMicrotreinamentoRepository repository) {
        this.repository = repository;
    }

    public NecessidadeMicrotreinamento identificarNecessidade(
            Long usuarioId,
            TipoErro tipoErro,
            List<LocalDateTime> ocorrencias) {

        LocalDateTime limitePeriodo =
                LocalDateTime.now()
                        .minusDays(CriterioErroRecorrente.PERIODO_DIAS);

        long quantidadeErrosRecentes = ocorrencias.stream()
                .filter(data -> !data.isBefore(limitePeriodo))
                .count();

        boolean necessario =
                quantidadeErrosRecentes >=
                        CriterioErroRecorrente.QUANTIDADE_MINIMA_ERROS;

        NecessidadeMicrotreinamento necessidade =
                new NecessidadeMicrotreinamento(
                        usuarioId,
                        tipoErro,
                        tipoErro.getTemaTreinamento(),
                        LocalDateTime.now(),
                        necessario
                );

        return repository.save(necessidade);
    }

    public List<NecessidadeMicrotreinamento> listarTodas() {
        return repository.findAll();
    }
}