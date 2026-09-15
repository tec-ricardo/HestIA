package br.com.hestia.ferramenta.service;

import br.com.hestia.ferramenta.model.FerramentaIA;
import br.com.hestia.ferramenta.model.NivelRiscoIA;
import br.com.hestia.ferramenta.repository.FerramentaIARepository;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class FerramentaIAServiceTest {

    @Test
    void deveListarFerramentasPorNivelDeRisco() {

        // Arrange
        FerramentaIARepository repository = mock(FerramentaIARepository.class);

        FerramentaIAService service =
                new FerramentaIAService(repository);

        when(repository.findByNivelRisco(NivelRiscoIA.ALTO))
                .thenReturn(List.of());

        // Action
        List<FerramentaIA> resultado =
                service.listarPorNivelRisco(NivelRiscoIA.ALTO);

        // Assert
        assertEquals(0, resultado.size());

        verify(repository).findByNivelRisco(NivelRiscoIA.ALTO);
    }
}