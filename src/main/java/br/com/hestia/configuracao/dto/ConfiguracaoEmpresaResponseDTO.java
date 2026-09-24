package br.com.hestia.configuracao.dto;

import br.com.hestia.configuracao.model.ConfiguracaoEmpresa;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ConfiguracaoEmpresaResponseDTO(Long empresaId, boolean registrarUsosAutomaticamente,
                                             boolean bloquearFerramentasNaoAprovadas,
                                             BigDecimal limiteCustoMensal, int diasRetencaoAuditoria,
                                             LocalDateTime atualizadaEm) {
    public static ConfiguracaoEmpresaResponseDTO from(ConfiguracaoEmpresa c) {
        return new ConfiguracaoEmpresaResponseDTO(c.getEmpresa().getId(), c.isRegistrarUsosAutomaticamente(),
                c.isBloquearFerramentasNaoAprovadas(), c.getLimiteCustoMensal(),
                c.getDiasRetencaoAuditoria(), c.getAtualizadaEm());
    }
}
