package br.com.hestia.gamificacao.model;

public enum AcaoExperiencia {
    REGISTRO_USO(10),
    USO_CONFORME(25),
    MICRO_TREINAMENTO(40),
    REUTILIZACAO_RESPOSTA(15),
    MISSAO_CONCLUIDA(50);

    private final int pontos;
    AcaoExperiencia(int pontos) { this.pontos = pontos; }
    public int getPontos() { return pontos; }
}
