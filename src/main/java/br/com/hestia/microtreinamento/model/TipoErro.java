package br.com.hestia.microtreinamento.model;

public enum TipoErro {

    DADOS_SENSIVEIS(TemaTreinamento.PROTECAO_DE_DADOS),
    FERRAMENTA_NAO_AUTORIZADA(TemaTreinamento.USO_DE_FERRAMENTAS_AUTORIZADAS),
    VIOLACAO_POLITICA_INTERNA(TemaTreinamento.POLITICAS_INTERNAS),
    FINALIDADE_INADEQUADA(TemaTreinamento.BOAS_PRATICAS_DE_USO),
    AUSENCIA_VALIDACAO_HUMANA(TemaTreinamento.VALIDACAO_HUMANA),
    USO_NAO_REGISTRADO(TemaTreinamento.REGISTRO_E_AUDITORIA),
    VIOLACAO_LGPD(TemaTreinamento.LGPD);

    private final TemaTreinamento temaTreinamento;

    TipoErro(TemaTreinamento temaTreinamento) {
        this.temaTreinamento = temaTreinamento;
    }

    public TemaTreinamento getTemaTreinamento() {
        return temaTreinamento;
    }
}