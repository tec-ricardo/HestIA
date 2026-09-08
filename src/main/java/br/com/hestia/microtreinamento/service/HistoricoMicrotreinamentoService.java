package br.com.hestia.microtreinamento.service;

import br.com.hestia.microtreinamento.model.ConclusaoMicrotreinamento;
import br.com.hestia.microtreinamento.model.HistoricoMicrotreinamento;
import br.com.hestia.microtreinamento.repository.HistoricoMicrotreinamentoRepository;
import org.springframework.stereotype.Service;

@Service
public class HistoricoMicrotreinamentoService {

    private final HistoricoMicrotreinamentoRepository repository;

    public HistoricoMicrotreinamentoService(
            HistoricoMicrotreinamentoRepository repository
    ) {
        this.repository = repository;
    }

    public HistoricoMicrotreinamento atualizarHistorico(
            ConclusaoMicrotreinamento conclusao
    ) {

        HistoricoMicrotreinamento historico =
                repository.findByUsuarioId(conclusao.getUsuarioId())
                        .orElseGet(() ->
                                new HistoricoMicrotreinamento(
                                        conclusao.getUsuarioId()
                                )
                        );

        historico.adicionarConclusao(conclusao);

        return repository.save(historico);
    }

    public HistoricoMicrotreinamento buscarPorUsuario(
            Long usuarioId
    ) {
        return repository.findByUsuarioId(usuarioId)
                .orElse(null);
    }
}