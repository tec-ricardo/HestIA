package br.com.hestia.reutilizavel.service;

import br.com.hestia.gamificacao.service.GamificacaoService;
import br.com.hestia.registro.repository.RegistroUsoIARepository;
import br.com.hestia.reutilizavel.dto.EconomiaReutilizacaoDTO;
import br.com.hestia.reutilizavel.model.RespostaReutilizavel;
import br.com.hestia.reutilizavel.repository.RespostaReutilizavelRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EconomiaReutilizacaoServiceTest {

    @Test
    void deveCalcularIndicadoresDeEconomia() {

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

        RespostaReutilizavel resposta1 =
                new RespostaReutilizavel();

        resposta1.setEmpresaId(1L);
        resposta1.setTokensEntrada(100);
        resposta1.setTokensSaida(50);
        resposta1.setCustoEstimado(
                new BigDecimal("0.20")
        );
        resposta1.prePersist();

        resposta1.registrarReutilizacao();
        resposta1.registrarReutilizacao();

        RespostaReutilizavel resposta2 =
                new RespostaReutilizavel();

        resposta2.setEmpresaId(1L);
        resposta2.setTokensEntrada(80);
        resposta2.setTokensSaida(20);
        resposta2.setCustoEstimado(
                new BigDecimal("0.10")
        );
        resposta2.prePersist();

        resposta2.registrarReutilizacao();

        when(repository.findByEmpresaId(1L))
                .thenReturn(
                        List.of(
                                resposta1,
                                resposta2
                        )
                );

        when(
                registroUsoIARepository
                        .countByUsuarioEmpresaId(1L)
        ).thenReturn(7L);

        EconomiaReutilizacaoDTO resultado =
                service.consultarEconomia(1L);

        assertEquals(
                3,
                resultado.chamadasEvitadas()
        );

        assertEquals(
                400L,
                resultado.tokensEconomizados()
        );

        assertEquals(
                0,
                new BigDecimal("0.50")
                        .compareTo(
                                resultado.custoEstimadoEvitado()
                        )
        );

        assertEquals(
                30.0,
                resultado.percentualReutilizacao(),
                0.0001
        );
    }

    @Test
    void deveRetornarZeroQuandoNaoExistiremUtilizacoes() {

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

        when(repository.findByEmpresaId(1L))
                .thenReturn(List.of());

        when(
                registroUsoIARepository
                        .countByUsuarioEmpresaId(1L)
        ).thenReturn(0L);

        EconomiaReutilizacaoDTO resultado =
                service.consultarEconomia(1L);

        assertEquals(
                0,
                resultado.chamadasEvitadas()
        );

        assertEquals(
                0L,
                resultado.tokensEconomizados()
        );

        assertEquals(
                0,
                BigDecimal.ZERO.compareTo(
                        resultado.custoEstimadoEvitado()
                )
        );

        assertEquals(
                0.0,
                resultado.percentualReutilizacao(),
                0.0001
        );
    }
}