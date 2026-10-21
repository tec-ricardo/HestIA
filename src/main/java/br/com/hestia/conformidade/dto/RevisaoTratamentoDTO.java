package br.com.hestia.conformidade.dto;

import br.com.hestia.conformidade.model.*;
import jakarta.validation.constraints.*;

public record RevisaoTratamentoDTO(
        @NotNull DecisaoRevisao decisao,
        StatusConformidade resultado,
        @NotBlank @Size(max = 1000) String justificativa,
        @NotNull @PositiveOrZero Long versaoRegistro) {}
