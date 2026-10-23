package br.com.hestia.gamificacao.dto;

import java.util.List;

public record AiCreditsResponseDTO(
        int saldo,
        Integer posicao,
        int participantes,
        String regra,
        List<ItemRanking> ranking) {

    public record ItemRanking(int posicao, Long usuarioId, String nome, String nivel,
                              int creditos, boolean usuarioAtual) {}
}
