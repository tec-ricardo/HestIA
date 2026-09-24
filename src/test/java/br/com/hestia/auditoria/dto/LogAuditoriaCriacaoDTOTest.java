package br.com.hestia.auditoria.dto;

import br.com.hestia.auditoria.model.ResultadoAuditoria;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LogAuditoriaCriacaoDTOTest {

    @Test
    void rejeitaDadosObrigatoriosInvalidos() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var dto = new LogAuditoriaCriacaoDTO(
                    1L, null, "", 10L,
                    "descricao", null, null, null
            );

            var campos = factory.getValidator().validate(dto).stream()
                    .map(v -> v.getPropertyPath().toString())
                    .collect(java.util.stream.Collectors.toSet());

            assertThat(campos).contains("acao", "entidade", "resultado");
        }
    }
}
