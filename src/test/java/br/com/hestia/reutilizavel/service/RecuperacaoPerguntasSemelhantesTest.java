package br.com.hestia.reutilizavel.service;

import br.com.hestia.gamificacao.service.GamificacaoService;
import br.com.hestia.registro.repository.RegistroUsoIARepository;
import br.com.hestia.reutilizavel.model.RespostaReutilizavel;
import br.com.hestia.reutilizavel.repository.RespostaReutilizavelRepository;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RecuperacaoPerguntasSemelhantesTest {

    @Test
    void deveRecuperarSomentePerguntasElegiveis() {

        RespostaReutilizavelRepository repository =
                mock(RespostaReutilizavelRepository.class);

        GamificacaoService gamificacaoService =
                mock(GamificacaoService.class);

        RegistroUsoIARepository registroUsoIARepository =
                mock(RegistroUsoIARepository.class);

        RespostaReutilizavelService service =
                new RespostaReutilizavelService(
                        repository,
                        gamificacaoService,
                        registroUsoIARepository
                );

        RespostaReutilizavel resposta =
                new RespostaReutilizavel();

        resposta.setEmpresaId(1L);
        resposta.setReutilizavel(true);
        resposta.setPossuiDadosSensiveis(false);
        resposta.setEmbedding("[0.1, 0.2, 0.3]");

        when(
                repository
                        .findByEmpresaIdAndReutilizavelTrueAndPossuiDadosSensiveisFalseAndEmbeddingIsNotNull(
                                1L
                        )
        ).thenReturn(List.of(resposta));

        List<RespostaReutilizavel> resultado =
                service.recuperarPerguntasSemelhantes(1L);

        assertEquals(1, resultado.size());
        assertEquals(
                1L,
                resultado.get(0).getEmpresaId()
        );

        verify(repository)
                .findByEmpresaIdAndReutilizavelTrueAndPossuiDadosSensiveisFalseAndEmbeddingIsNotNull(
                        1L
                );
    }
}