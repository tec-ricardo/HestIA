package br.com.hestia.microtreinamento.service;

import br.com.hestia.microtreinamento.model.ConclusaoMicrotreinamento;
import br.com.hestia.microtreinamento.repository.ConclusaoMicrotreinamentoRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ConclusaoMicrotreinamentoService {

    private final ConclusaoMicrotreinamentoRepository repository;
    private final HistoricoMicrotreinamentoService historicoService;

    public ConclusaoMicrotreinamentoService(
            ConclusaoMicrotreinamentoRepository repository,
            HistoricoMicrotreinamentoService historicoService
    ) {
        this.repository = repository;
        this.historicoService = historicoService;
    }

    public ConclusaoMicrotreinamento registrarConclusao(
            Long usuarioId,
            Long microtreinamentoId
    ) {

        ConclusaoMicrotreinamento conclusao =
                new ConclusaoMicrotreinamento(
                        usuarioId,
                        microtreinamentoId,
                        LocalDateTime.now(),
                        true
                );

        ConclusaoMicrotreinamento conclusaoSalva =
                repository.save(conclusao);

        historicoService.atualizarHistorico(conclusaoSalva);

        return conclusaoSalva;
    }

    public List<ConclusaoMicrotreinamento> listarConclusoes() {
        return repository.findAll();
    }
}