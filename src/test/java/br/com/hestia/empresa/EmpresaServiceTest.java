package br.com.hestia.empresa;

import br.com.hestia.empresa.dto.EmpresaDTO;
import br.com.hestia.empresa.model.Empresa;
import br.com.hestia.empresa.repository.EmpresaRepository;
import br.com.hestia.empresa.service.EmpresaService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EmpresaServiceTest {

    @Test
    void naoDeveCadastrarEmpresaComCnpjDuplicado() {

        // ARRANGE
        EmpresaRepository empresaRepository = mock(EmpresaRepository.class);
        EmpresaService empresaService = new EmpresaService(empresaRepository);

        EmpresaDTO dto = new EmpresaDTO();
        dto.setNome("Empresa Teste");
        dto.setCnpj("12345678000199");
        dto.setConfiguracoesGerais("Configuração");
        dto.setPoliticas("Política");
        dto.setOrcamento(10000.0);

        when(empresaRepository.existsByCnpj("12345678000199"))
                .thenReturn(true);

        // ACT + ASSERT
        assertThrows(
                IllegalArgumentException.class,
                () -> empresaService.cadastrar(dto)
        );
    }
}