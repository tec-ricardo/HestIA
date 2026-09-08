package br.com.hestia.microtreinamento.service;

import br.com.hestia.microtreinamento.model.ConclusaoMicrotreinamento;
import br.com.hestia.microtreinamento.model.HistoricoMicrotreinamento;
import br.com.hestia.microtreinamento.repository.HistoricoMicrotreinamentoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HistoricoMicrotreinamentoService {

    private final HistoricoMicrotreinamentoRepository repository;

    public HistoricoMicrotreinamentoService(
            HistoricoMicrotreinamentoRepository repository
    ) {
        this.repository = repository;
    }

    @Transactional(propagation = Propagation.MANDATORY)
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

        boolean jaRegistrada =
                historico.getConclusoes().stream()
                        .anyMatch(item ->
                                item.getId().equals(conclusao.getId())
                        );

        if (!jaRegistrada) {
            historico.adicionarConclusao(conclusao);
        }

        return repository.save(historico);
    }

    @Transactional(readOnly = true)
    public HistoricoMicrotreinamento buscarPorUsuario(
            Long usuarioId
    ) {
        HistoricoMicrotreinamento historico =
                repository.findByUsuarioId(usuarioId)
                        .orElse(null);

        if (historico != null) {
            // Carrega a lista enquanto a transação está aberta.
            historico.getConclusoes().size();
        }

        return historico;
    }
}