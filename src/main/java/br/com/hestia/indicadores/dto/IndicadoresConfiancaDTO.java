package br.com.hestia.indicadores.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record IndicadoresConfiancaDTO(
        Long empresaId, String visao, Long usuarioId, Long departamentoId,
        PeriodoReferenciaDTO periodo, LocalDateTime apuradoEm, String naturezaDados,
        String versaoComposicao, String versaoConfianca, BigDecimal scoreGlobal,
        BigDecimal scoreParcial, BigDecimal coberturaPesos,
        List<Indicador> indicadores, List<String> limitacoes) {
    public IndicadoresConfiancaDTO {
        indicadores = List.copyOf(indicadores);
        limitacoes = List.copyOf(limitacoes);
    }
    public record Indicador(String codigo, String nome, BigDecimal peso, BigDecimal valor,
            String unidade, String fonte, String formula, Long numerador, Long denominador,
            ConfiancaIndicadorDTO confianca) {}
}
