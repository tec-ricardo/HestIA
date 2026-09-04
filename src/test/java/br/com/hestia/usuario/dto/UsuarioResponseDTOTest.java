package br.com.hestia.usuario.dto;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class UsuarioResponseDTOTest {

    @Test
    void contratoDeRespostaNaoExpoeSenha() {
        assertThat(Arrays.stream(UsuarioResponseDTO.class.getRecordComponents())
                .map(component -> component.getName()))
                .doesNotContain("senha");
    }
}
