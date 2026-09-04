package br.com.hestia.empresa.service;

import br.com.hestia.empresa.dto.EmpresaDTO;
import br.com.hestia.empresa.repository.EmpresaRepository;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EmpresaServiceTest {

    private final EmpresaRepository repository = mock(EmpresaRepository.class);
    private final EmpresaService service = new EmpresaService(repository);

    @Test
    void impedeCnpjDuplicado() {
        var dto = new EmpresaDTO("ESPM", "61.428.598/0001-90", null, 1000.0);
        when(repository.existsByCnpj("61428598000190")).thenReturn(true);

        assertThatThrownBy(() -> service.cadastrar(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("CNPJ já cadastrado");

        verify(repository).existsByCnpj("61428598000190");
    }
}
