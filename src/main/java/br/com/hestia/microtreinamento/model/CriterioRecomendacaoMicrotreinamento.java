package br.com.hestia.microtreinamento.model;

public final class CriterioRecomendacaoMicrotreinamento {

    private CriterioRecomendacaoMicrotreinamento() {
    }

    public static boolean deveRecomendar(NecessidadeMicrotreinamento necessidade) {
        return necessidade != null && necessidade.isNecessario();
    }

    public static TemaTreinamento identificarTema(NecessidadeMicrotreinamento necessidade) {

        if (necessidade == null || necessidade.getTipoErro() == null) {
            return null;
        }

        return necessidade.getTipoErro().getTemaTreinamento();
    }
}