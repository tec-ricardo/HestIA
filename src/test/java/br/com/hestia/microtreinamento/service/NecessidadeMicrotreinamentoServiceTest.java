package br.com.hestia.microtreinamento.service;

import br.com.hestia.microtreinamento.model.NecessidadeMicrotreinamento;
import br.com.hestia.microtreinamento.model.TipoErro;
import br.com.hestia.microtreinamento.repository.NecessidadeMicrotreinamentoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NecessidadeMicrotreinamentoServiceTest {

    private NecessidadeMicrotreinamentoRepository repository;
    private NecessidadeMicrotreinamentoService service;

    @BeforeEach
    void setUp() {
        repository = mock(NecessidadeMicrotreinamentoRepository.class);
        service = new NecessidadeMicrotreinamentoService(repository);

        when(repository.save(any(NecessidadeMicrotreinamento.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void deveIdentificarNecessidadeQuandoUsuarioTiverTresErrosNosUltimos30Dias() {

        List<LocalDateTime> ocorrencias = List.of(
                LocalDateTime.now().minusDays(5),
                LocalDateTime.now().minusDays(10),
                LocalDateTime.now().minusDays(20)
        );

        NecessidadeMicrotreinamento resultado =
                service.identificarNecessidade(
                        1L,
                        TipoErro.DADOS_SENSIVEIS,
                        ocorrencias
                );

        assertTrue(resultado.isNecessario());
        assertEquals(TipoErro.DADOS_SENSIVEIS, resultado.getTipoErro());

        verify(repository, times(1))
                .save(any(NecessidadeMicrotreinamento.class));
    }

    @Test
    void naoDeveIdentificarNecessidadeQuandoUsuarioTiverApenasDoisErros() {

        List<LocalDateTime> ocorrencias = List.of(
                LocalDateTime.now().minusDays(5),
                LocalDateTime.now().minusDays(10)
        );

        NecessidadeMicrotreinamento resultado =
                service.identificarNecessidade(
                        1L,
                        TipoErro.DADOS_SENSIVEIS,
                        ocorrencias
                );

        assertFalse(resultado.isNecessario());
    }

    @Test
    void deveIgnorarErrosComMaisDe30Dias() {

        List<LocalDateTime> ocorrencias = List.of(
                LocalDateTime.now().minusDays(5),
                LocalDateTime.now().minusDays(10),
                LocalDateTime.now().minusDays(40)
        );

        NecessidadeMicrotreinamento resultado =
                service.identificarNecessidade(
                        1L,
                        TipoErro.DADOS_SENSIVEIS,
                        ocorrencias
                );

        assertFalse(resultado.isNecessario());
    }

    @Test
    void deveRelacionarErroAoTemaCorretoDeTreinamento() {

        List<LocalDateTime> ocorrencias = List.of(
                LocalDateTime.now().minusDays(2),
                LocalDateTime.now().minusDays(4),
                LocalDateTime.now().minusDays(6)
        );

        NecessidadeMicrotreinamento resultado =
                service.identificarNecessidade(
                        1L,
                        TipoErro.VIOLACAO_LGPD,
                        ocorrencias
                );

        assertTrue(resultado.isNecessario());

        assertEquals(
                TipoErro.VIOLACAO_LGPD.getTemaTreinamento(),
                resultado.getTemaTreinamento()
        );
    }
}