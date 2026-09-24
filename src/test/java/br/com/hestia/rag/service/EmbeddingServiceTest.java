package br.com.hestia.rag.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class EmbeddingServiceTest {

    @Test
    void deveGerarEmbeddingCom1536Dimensoes() {

        EmbeddingService service = new EmbeddingService();

        List<Float> embedding = service.gerarEmbedding(
                "Como utilizar inteligência artificial de forma segura?"
        );

        assertFalse(embedding.isEmpty());
        assertEquals(1536, embedding.size());
    }
}