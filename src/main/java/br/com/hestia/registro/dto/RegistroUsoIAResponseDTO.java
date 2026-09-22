package br.com.hestia.registro.dto;

import br.com.hestia.registro.model.FinalidadeUso;
import br.com.hestia.registro.model.RegistroUsoIA;
import br.com.hestia.registro.model.StatusExecucao;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record RegistroUsoIAResponseDTO(
        Long id,
        Long usuarioId,
        String usuarioNome,
        Long ferramentaId,
        String ferramentaNome,
        String modeloIa,
        LocalDateTime dataHora,
        FinalidadeUso finalidade,
        Integer tokensEntrada,
        Integer tokensSaida,
        Integer tokensTotal,
        BigDecimal custoEstimado,
        StatusExecucao statusExecucao,
        Long tempoRespostaMs
) {
    public static RegistroUsoIAResponseDTO from(RegistroUsoIA registro) {
        return new RegistroUsoIAResponseDTO(
                registro.getId(),
                registro.getUsuario().getId(),
                registro.getUsuario().getNome(),
                registro.getFerramenta().getId(),
                registro.getFerramenta().getNome(),
                registro.getModeloIa(),
                registro.getDataHora(),
                registro.getFinalidade(),
                registro.getTokensEntrada(),
                registro.getTokensSaida(),
                registro.getTokensTotal(),
                registro.getCustoEstimado(),
                registro.getStatusExecucao(),
                registro.getTempoRespostaMs());
    }
}
