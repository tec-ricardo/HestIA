package br.com.hestia.microtreinamento.service;

import br.com.hestia.microtreinamento.model.CriterioRecomendacaoMicrotreinamento;
import br.com.hestia.microtreinamento.model.Microtreinamento;
import br.com.hestia.microtreinamento.model.NecessidadeMicrotreinamento;
import br.com.hestia.microtreinamento.model.TipoErro;

import java.util.List;

public class RecomendacaoMicrotreinamentoService {

    private final MicrotreinamentoFicticioService microtreinamentoFicticioService;

    public RecomendacaoMicrotreinamentoService() {
        this.microtreinamentoFicticioService =
                new MicrotreinamentoFicticioService();
    }

    public Microtreinamento recomendar(
            NecessidadeMicrotreinamento necessidade) {

        if (!CriterioRecomendacaoMicrotreinamento
                .deveRecomendar(necessidade)) {
            return null;
        }

        return buscarTreinamentoPorErro(
                necessidade.getTipoErro()
        );
    }

    public Microtreinamento buscarTreinamentoPorErro(
            TipoErro tipoErro) {

        if (tipoErro == null) {
            return null;
        }

        List<Microtreinamento> treinamentos =
                microtreinamentoFicticioService
                        .criarTreinamentos();

        return treinamentos.stream()
                .filter(treinamento ->
                        treinamento.getTema()
                                == tipoErro.getTemaTreinamento())
                .findFirst()
                .orElse(null);
    }
}