package br.com.hestia.microtreinamento.service;

import br.com.hestia.microtreinamento.model.ConclusaoMicrotreinamento;
import br.com.hestia.microtreinamento.repository.ConclusaoMicrotreinamentoRepository;
import br.com.hestia.usuario.model.Usuario;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;

@Service
public class ConclusaoMicrotreinamentoService {

    private final ConclusaoMicrotreinamentoRepository repository;
    private final HistoricoMicrotreinamentoService historicoService;
    private final EntityManager entityManager;

    private final MicrotreinamentoFicticioService catalogo =
            new MicrotreinamentoFicticioService();

    public ConclusaoMicrotreinamentoService(
            ConclusaoMicrotreinamentoRepository repository,
            HistoricoMicrotreinamentoService historicoService,
            EntityManager entityManager
    ) {
        this.repository = repository;
        this.historicoService = historicoService;
        this.entityManager = entityManager;
    }

    @Transactional
    public ConclusaoMicrotreinamento registrarConclusao(
            Long usuarioId,
            Long microtreinamentoId
    ) {
        if (usuarioId == null || usuarioId <= 0
                || microtreinamentoId == null
                || microtreinamentoId <= 0) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Informe IDs positivos para usuário e microtreinamento."
            );
        }

        // Serializa registros simultâneos para o mesmo usuário.
        Usuario usuario = entityManager.find(
                Usuario.class,
                usuarioId,
                LockModeType.PESSIMISTIC_WRITE
        );

        if (usuario == null) {
            throw new NoSuchElementException(
                    "Usuário não encontrado."
            );
        }

        var existente =
                repository.findByUsuarioIdAndMicrotreinamentoId(
                        usuarioId,
                        microtreinamentoId
                );

        if (existente.isPresent()) {
            return existente.get();
        }

        boolean treinamentoValido =
                catalogo.criarTreinamentos().stream()
                        .anyMatch(treinamento ->
                                microtreinamentoId.equals(
                                        treinamento.getId()
                                ) && treinamento.isAtivo()
                        );

        if (!treinamentoValido) {
            throw new NoSuchElementException(
                    "Microtreinamento ativo não encontrado."
            );
        }

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

    @Transactional(readOnly = true)
    public List<ConclusaoMicrotreinamento> listarConclusoes() {
        return repository.findAll();
    }
}