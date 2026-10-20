package br.com.hestia.rag.service;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.embeddings.EmbeddingCreateParams;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EmbeddingService {

    private static final String MODELO = "text-embedding-3-small";

    public List<Float> gerarEmbedding(String texto) {

        if (texto == null || texto.isBlank()) {
            throw new IllegalArgumentException("O texto não pode estar vazio.");
        }

        OpenAIClient client = OpenAIOkHttpClient.fromEnv();

        var resposta = client.embeddings()
                .create(
                        EmbeddingCreateParams.builder()
                                .model(MODELO)
                                .input(texto)
                                .build()
                );

        return resposta.data()
                .get(0)
                .embedding();
    }
}