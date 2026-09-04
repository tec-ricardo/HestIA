package br.com.hestia.ferramenta.dto;

import br.com.hestia.ferramenta.model.FerramentaIA;
import br.com.hestia.ferramenta.model.NivelRiscoIA;
import br.com.hestia.ferramenta.model.StatusFerramentaIA;
import br.com.hestia.ferramenta.model.TipoFerramentaIA;

import java.time.LocalDateTime;

public record FerramentaIAResponseDTO(
        Long id,
        String nome,
        String fornecedor,
        String descricao,
        TipoFerramentaIA tipo,
        String finalidadeUso,
        String urlAcesso,
        StatusFerramentaIA status,
        NivelRiscoIA nivelRisco,
        Boolean trataDadosPessoais,
        LocalDateTime dataCadastro,
        LocalDateTime dataAtualizacao,
        Long empresaId
) {
    public static FerramentaIAResponseDTO from(FerramentaIA ferramenta) {
        return new FerramentaIAResponseDTO(
                ferramenta.getId(), ferramenta.getNome(), ferramenta.getFornecedor(),
                ferramenta.getDescricao(), ferramenta.getTipo(), ferramenta.getFinalidadeUso(),
                ferramenta.getUrlAcesso(), ferramenta.getStatus(), ferramenta.getNivelRisco(),
                ferramenta.getTrataDadosPessoais(), ferramenta.getDataCadastro(),
                ferramenta.getDataAtualizacao(), ferramenta.getEmpresa().getId()
        );
    }
}
