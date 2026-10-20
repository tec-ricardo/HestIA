package br.com.hestia.rag.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LimpezaDivisaoTextoServiceTest {

    private final LimpezaDivisaoTextoService service =
            new LimpezaDivisaoTextoService();

    @Test
    void deveLimparEspacosQuebrasDeLinhaEHtml() {
        String resultado = service.limpar(
                "  <p>Política   interna</p>\n\n de uso  "
        );

        assertEquals("Política interna de uso", resultado);
    }

    @Test
    void deveDividirTextoRespeitandoTamanhoMaximo() {
        List<String> partes = service.dividir(
                "A política define regras para utilização segura de ferramentas de inteligência artificial.",
                35,
                5
        );

        assertTrue(partes.size() > 1);

        partes.forEach(parte ->
                assertTrue(parte.length() <= 35)
        );
    }

    @Test
    void deveRetornarTextoInteiroQuandoMenorQueLimite() {
        List<String> partes = service.dividir(
                "Texto curto.",
                100,
                10
        );

        assertEquals(List.of("Texto curto."), partes);
    }

    @Test
    void deveRejeitarTextoVazio() {
        assertThrows(
                IllegalArgumentException.class,
                () -> service.limpar("   ")
        );
    }

    @Test
    void deveRejeitarSobreposicaoInvalida() {
        assertThrows(
                IllegalArgumentException.class,
                () -> service.dividir("Texto válido", 10, 10)
        );
    }
}