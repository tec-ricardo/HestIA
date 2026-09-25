package br.com.hestia.reutilizavel;

import br.com.hestia.reutilizavel.model.RespostaReutilizavel;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class EconomiaReutilizacaoTest {

    @Test
    void deveContabilizarChamadaEvitadaETokensEconomizados() {

        RespostaReutilizavel resposta =
                new RespostaReutilizavel();

        resposta.setTokensEntrada(120);
        resposta.setTokensSaida(80);
        resposta.prePersist();

        resposta.registrarReutilizacao();
        resposta.registrarReutilizacao();

        assertEquals(2, resposta.getChamadasEvitadas());
        assertEquals(400L, resposta.getTokensEconomizados());
    }

    @Test
    void respostaPodeSerDesabilitadaParaReutilizacao() {

        RespostaReutilizavel resposta =
                new RespostaReutilizavel();

        resposta.setReutilizavel(false);
        resposta.prePersist();

        assertFalse(resposta.getReutilizavel());
    }

    @Test
    void deveCalcularCustoEstimadoEvitado() {

        RespostaReutilizavel resposta =
                new RespostaReutilizavel();

        resposta.setCustoEstimado(
                new BigDecimal("0.25")
        );

        resposta.prePersist();

        resposta.registrarReutilizacao();
        resposta.registrarReutilizacao();

        assertEquals(
                0,
                new BigDecimal("0.50")
                        .compareTo(
                                resposta.getCustoEstimadoEvitado()
                        )
        );
    }
}