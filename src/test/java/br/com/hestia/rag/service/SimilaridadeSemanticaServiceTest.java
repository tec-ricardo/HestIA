package br.com.hestia.rag.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

class SimilaridadeSemanticaServiceTest {

    @Test
    void deveCalcularSimilaridadeEntreEmbeddingsIguais() {

        EmbeddingService embeddingService = mock(EmbeddingService.class);

        SimilaridadeSemanticaService service =
                new SimilaridadeSemanticaService(embeddingService);

        List<Float> embedding1 = List.of(1.0f, 2.0f, 3.0f);
        List<Float> embedding2 = List.of(1.0f, 2.0f, 3.0f);

        double similaridade =
                service.calcularSimilaridade(
                        embedding1,
                        embedding2
                );

        assertEquals(
                1.0,
                similaridade,
                0.0001
        );
    }

    @Test
    void deveRetornarZeroParaEmbeddingsOrtogonais() {

        EmbeddingService embeddingService = mock(EmbeddingService.class);

        SimilaridadeSemanticaService service =
                new SimilaridadeSemanticaService(embeddingService);

        List<Float> embedding1 = List.of(1.0f, 0.0f);
        List<Float> embedding2 = List.of(0.0f, 1.0f);

        double similaridade =
                service.calcularSimilaridade(
                        embedding1,
                        embedding2
                );

        assertEquals(
                0.0,
                similaridade,
                0.0001
        );
    }

    @Test
    void deveRejeitarEmbeddingsComDimensoesDiferentes() {

        EmbeddingService embeddingService = mock(EmbeddingService.class);

        SimilaridadeSemanticaService service =
                new SimilaridadeSemanticaService(embeddingService);

        List<Float> embedding1 =
                List.of(1.0f, 2.0f, 3.0f);

        List<Float> embedding2 =
                List.of(1.0f, 2.0f);

        assertThrows(
                IllegalArgumentException.class,
                () -> service.calcularSimilaridade(
                        embedding1,
                        embedding2
                )
        );
    }
}