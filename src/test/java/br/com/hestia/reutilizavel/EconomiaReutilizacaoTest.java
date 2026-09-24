package br.com.hestia.reutilizavel;

import br.com.hestia.reutilizavel.model.RespostaReutilizavel;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EconomiaReutilizacaoTest {
    @Test
    void deveContabilizarChamadaEvitadaETokensEconomizados() {
        RespostaReutilizavel resposta = new RespostaReutilizavel();
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
        RespostaReutilizavel resposta = new RespostaReutilizavel();
        resposta.setReutilizavel(false);
        resposta.prePersist();

        assertFalse(resposta.getReutilizavel());
    }
}
