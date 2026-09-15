package br.com.hestia.departamento.controller;

import br.com.hestia.departamento.dto.DepartamentoDTO;
import br.com.hestia.departamento.model.Departamento;
import br.com.hestia.departamento.service.DepartamentoService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DepartamentoControllerTest {

    @Mock
    private DepartamentoService departamentoService;

    @InjectMocks
    private DepartamentoController departamentoController;

    @Test
    void deveRetornarHttp201QuandoDepartamentoForCadastrado() {
        // Arrange
        DepartamentoDTO dto = new DepartamentoDTO(
                "Tecnologia",
                "Ricardo",
                "Diretoria > Tecnologia",
                10L);
        Departamento departamento = new Departamento();
        departamento.setId(1L);
        departamento.setNome("Tecnologia");
        when(departamentoService.cadastrar(dto)).thenReturn(departamento);

        // Act
        ResponseEntity<Departamento> resposta =
                departamentoController.cadastrar(dto);

        // Assert
        assertEquals(HttpStatus.CREATED, resposta.getStatusCode());
        assertSame(departamento, resposta.getBody());
    }
}
