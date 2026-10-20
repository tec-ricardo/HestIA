package br.com.hestia.rag.service;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SimilaridadeSemanticaService {

    private final EmbeddingService embeddingService;

    public SimilaridadeSemanticaService(EmbeddingService embeddingService) {
        this.embeddingService = embeddingService;
    }

    public double calcularEntrePerguntas(String pergunta1, String pergunta2) {

        if (pergunta1 == null || pergunta1.isBlank()
                || pergunta2 == null || pergunta2.isBlank()) {
            throw new IllegalArgumentException("As perguntas não podem estar vazias.");
        }

        List<Float> embedding1 = embeddingService.gerarEmbedding(pergunta1);
        List<Float> embedding2 = embeddingService.gerarEmbedding(pergunta2);

        return calcularSimilaridade(embedding1, embedding2);
    }

    public double calcularSimilaridade(
            List<Float> embedding1,
            List<Float> embedding2
    ) {

        if (embedding1 == null || embedding2 == null
                || embedding1.isEmpty() || embedding2.isEmpty()) {
            throw new IllegalArgumentException("Os embeddings não podem estar vazios.");
        }

        if (embedding1.size() != embedding2.size()) {
            throw new IllegalArgumentException(
                    "Os embeddings devem possuir a mesma dimensão."
            );
        }

        double produtoEscalar = 0;
        double norma1 = 0;
        double norma2 = 0;

        for (int i = 0; i < embedding1.size(); i++) {
            double valor1 = embedding1.get(i);
            double valor2 = embedding2.get(i);

            produtoEscalar += valor1 * valor2;
            norma1 += valor1 * valor1;
            norma2 += valor2 * valor2;
        }

        if (norma1 == 0 || norma2 == 0) {
            return 0;
        }

        return produtoEscalar
                / (Math.sqrt(norma1) * Math.sqrt(norma2));
    }
}