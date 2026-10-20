package br.com.hestia.rag.service;

import br.com.hestia.empresa.model.Empresa;
import br.com.hestia.rag.dto.ResultadoReindexacaoDTO.Status;
import br.com.hestia.reutilizavel.repository.RespostaReutilizavelRepository;
import br.com.hestia.reutilizavel.service.AcessoRespostaService;
import br.com.hestia.usuario.model.PerfilUsuario;
import br.com.hestia.usuario.model.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ReindexacaoServiceTest {

    private BuscaVetorialService busca;
    private ReindexacaoService service;
    private Usuario usuario;

    @BeforeEach
    void preparar() {
        busca = mock(BuscaVetorialService.class);

        var acesso = new AcessoRespostaService(
                mock(RespostaReutilizavelRepository.class)
        );

        service = new ReindexacaoService(busca, acesso);

        usuario = new Usuario();
        usuario.setId(10L);
        usuario.setAtivo(true);
        usuario.setPerfil(PerfilUsuario.FUNCIONARIO);
        usuario.setEmpresa(
                new Empresa(7L, "Empresa", "12345678901234", null, null)
        );
    }

    @Test
    void deveUsarEmpresaDaSessaoEProcessarCadaIdentificadorUmaVez() {
        var resultado = service.reindexar(
                usuario, List.of(1L, 2L, 1L)
        );

        assertEquals(List.of(1L, 2L), resultado.stream()
                .map(item -> item.respostaId())
                .toList());

        assertTrue(resultado.stream()
                .allMatch(item -> item.status() == Status.INDEXADA));

        verify(busca).indexarResposta(7L, 1L);
        verify(busca).indexarResposta(7L, 2L);
        verifyNoMoreInteractions(busca);
    }

    @Test
    void deveContinuarLoteAposFalhasIndividuais() {
        doThrow(new IllegalArgumentException("Indisponível"))
                .when(busca).indexarResposta(7L, 2L);

        doThrow(new ResponseStatusException(HttpStatus.CONFLICT))
                .when(busca).indexarResposta(7L, 3L);

        doThrow(new IllegalStateException("Falha do provedor"))
                .when(busca).indexarResposta(7L, 4L);

        var resultado = service.reindexar(
                usuario, List.of(1L, 2L, 3L, 4L, 5L)
        );

        assertEquals(
                List.of(
                        Status.INDEXADA,
                        Status.INDISPONIVEL,
                        Status.CONFLITO,
                        Status.FALHA,
                        Status.INDEXADA
                ),
                resultado.stream().map(item -> item.status()).toList()
        );

        verify(busca).indexarResposta(7L, 5L);
    }

    @Test
    void deveInterromperLoteQuandoAutorizacaoForRevogada() {
        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN))
                .when(busca).indexarResposta(7L, 2L);

        var erro = assertThrows(
                ResponseStatusException.class,
                () -> service.reindexar(usuario, List.of(1L, 2L, 3L))
        );

        assertEquals(403, erro.getStatusCode().value());
        verify(busca).indexarResposta(7L, 1L);
        verify(busca, never()).indexarResposta(7L, 3L);
    }

    @Test
    void deveRejeitarLotesInvalidosAntesDaIndexacao() {
        assertThrows(
                ResponseStatusException.class,
                () -> service.reindexar(usuario, null)
        );

        for (List<Long> ids : List.<List<Long>>of(
                List.of(),
                List.of(0L),
                List.of(-1L),
                Arrays.asList(1L, null),
                Collections.nCopies(21, 1L)
        )) {
            assertThrows(
                    ResponseStatusException.class,
                    () -> service.reindexar(usuario, ids)
            );
        }

        verifyNoInteractions(busca);
    }

    @Test
    void deveBloquearUsuarioInativoAntesDeProcessarLote() {
        usuario.setAtivo(false);

        assertThrows(
                ResponseStatusException.class,
                () -> service.reindexar(usuario, List.of(1L))
        );

        verifyNoInteractions(busca);
    }
}