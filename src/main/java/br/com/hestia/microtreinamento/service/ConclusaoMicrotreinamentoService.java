package br.com.hestia.microtreinamento.service;

import br.com.hestia.auditoria.model.ResultadoAuditoria;
import br.com.hestia.auditoria.model.TipoAcaoAuditoria;
import br.com.hestia.auditoria.service.LogAuditoriaService;
import br.com.hestia.microtreinamento.model.ConclusaoMicrotreinamento;
import br.com.hestia.microtreinamento.repository.ConclusaoMicrotreinamentoRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ConclusaoMicrotreinamentoService {

    private final ConclusaoMicrotreinamentoRepository repository;

    private final HistoricoMicrotreinamentoService
            historicoService;

    private final LogAuditoriaService
            logAuditoriaService;

    public ConclusaoMicrotreinamentoService(
            ConclusaoMicrotreinamentoRepository repository,
            HistoricoMicrotreinamentoService historicoService,
            LogAuditoriaService logAuditoriaService
    ) {
        this.repository = repository;

        this.historicoService =
                historicoService;

        this.logAuditoriaService =
                logAuditoriaService;
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

        historicoService
                .atualizarHistorico(
                        conclusaoSalva
                );

        logAuditoriaService.registrar(
                usuarioId,
                TipoAcaoAuditoria.CONCLUIR_TREINAMENTO,
                "MICROTREINAMENTO",
                microtreinamentoId,
                "Microtreinamento concluído",
                ResultadoAuditoria.SUCESSO,
                null,
                "conclusaoId="
                        + conclusaoSalva.getId()
        );

        return conclusaoSalva;
    }

    public List<ConclusaoMicrotreinamento>
    listarConclusoes() {

        return repository.findAll();
    }
}