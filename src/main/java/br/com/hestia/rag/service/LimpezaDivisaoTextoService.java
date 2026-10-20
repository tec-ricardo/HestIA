package br.com.hestia.rag.service;

import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;

@Service
public class LimpezaDivisaoTextoService {

    public String limpar(String texto) {
        if (texto == null || texto.isBlank()) {
            throw new IllegalArgumentException("O texto não pode estar vazio.");
        }

        String limpo = Normalizer.normalize(texto, Normalizer.Form.NFC)
                .replaceAll("<[^>]+>", " ")
                .replace("&nbsp;", " ")
                .replaceAll("[\\u200B-\\u200D\\uFEFF]", "")
                .replaceAll("[\\p{Cc}&&[^\\r\\n\\t]]", " ")
                .replaceAll("\\s+", " ")
                .trim();

        if (limpo.isEmpty()) {
            throw new IllegalArgumentException("O texto não possui conteúdo válido.");
        }

        return limpo;
    }

    public List<String> dividir(
            String texto,
            int tamanhoMaximo,
            int sobreposicao
    ) {
        if (tamanhoMaximo < 1) {
            throw new IllegalArgumentException(
                    "O tamanho máximo deve ser maior que zero."
            );
        }

        if (sobreposicao < 0 || sobreposicao >= tamanhoMaximo) {
            throw new IllegalArgumentException(
                    "A sobreposição deve estar entre zero e o tamanho máximo."
            );
        }

        String limpo = limpar(texto);

        if (limpo.length() <= tamanhoMaximo) {
            return List.of(limpo);
        }

        List<String> partes = new ArrayList<>();
        int inicio = 0;

        while (inicio < limpo.length()) {
            int fim = Math.min(inicio + tamanhoMaximo, limpo.length());

            if (fim < limpo.length()) {
                int limite = limpo.lastIndexOf(' ', fim);

                if (limite > inicio) {
                    fim = limite;
                }
            }

            String parte = limpo.substring(inicio, fim).trim();

            if (!parte.isEmpty()) {
                partes.add(parte);
            }

            if (fim >= limpo.length()) {
                break;
            }

            inicio = Math.max(0, fim - sobreposicao);

            while (inicio < limpo.length()
                    && Character.isWhitespace(limpo.charAt(inicio))) {
                inicio++;
            }
        }

        return List.copyOf(partes);
    }
}