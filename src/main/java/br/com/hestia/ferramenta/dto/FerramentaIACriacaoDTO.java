package br.com.hestia.ferramenta.dto;

import br.com.hestia.ferramenta.model.TipoFerramentaIA;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

public record FerramentaIACriacaoDTO(
        @NotBlank @Size(min = 2, max = 120) String nome,
        @NotBlank @Size(min = 2, max = 120) String fornecedor,
        @Size(max = 500) String descricao,
        @NotNull TipoFerramentaIA tipo,
        @NotBlank @Size(min = 10, max = 500) String finalidadeUso,
        @Size(max = 2048) @URL String urlAcesso,
        @NotNull Boolean trataDadosPessoais,
        @NotNull Long empresaId
) {
}
