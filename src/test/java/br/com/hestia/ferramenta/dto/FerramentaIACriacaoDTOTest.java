package br.com.hestia.ferramenta.dto;

import br.com.hestia.ferramenta.model.TipoFerramentaIA;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FerramentaIACriacaoDTOTest {

    @Test
    void rejeitaDadosInvalidos() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var dto = new FerramentaIACriacaoDTO(
                    "A", "", null, TipoFerramentaIA.CHATBOT,
                    "curta", "nao-e-url", null, null
            );

            var campos = factory.getValidator().validate(dto).stream()
                    .map(v -> v.getPropertyPath().toString())
                    .collect(java.util.stream.Collectors.toSet());

            assertThat(campos).contains(
                    "nome", "fornecedor", "finalidadeUso", "urlAcesso",
                    "trataDadosPessoais", "empresaId"
            );
        }
    }
}
