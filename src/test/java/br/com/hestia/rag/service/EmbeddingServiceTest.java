package br.com.hestia.rag.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class EmbeddingServiceTest {

    @Test
    void deveGerarEmbeddingCom1536Dimensoes() {

        String apiKey = System.getenv("OPENAI_API_KEY");

        assumeTrue(
                apiKey != null && !apiKey.isBlank(),
                "OPENAI_API_KEY não configurada"
        );

        EmbeddingService service = new EmbeddingService();

        List<Float> embedding = service.gerarEmbedding(
                "Como utilizar inteligência artificial de forma segura?"
        );

        assertFalse(embedding.isEmpty());
        assertEquals(1536, embedding.size());
    }
}