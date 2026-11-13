package br.com.hestia.rag.service;
import java.util.ArrayList;
import java.util.List;
/** Segmentos complementam o documento inteiro; autorização e versão continuam na fonte. */
final class TrechosDocumentoRag {
    private TrechosDocumentoRag() {}
    static List<String> dividir(String texto) {
        if (texto == null || texto.isBlank()) return List.of();
        var blocos = texto.split("(?m)(?=^#{1,6}\\s+)|\n\\s*\n");
        if (blocos.length < 2) return List.of();
        var saida = new ArrayList<String>();
        StringBuilder atual = new StringBuilder();
        for (var bloco : blocos) {
            bloco = bloco.strip();
            if (bloco.isEmpty()) continue;
            if (atual.length() > 0 && (atual.length() + bloco.length() > 1200 || bloco.startsWith("#"))) {
                saida.add(atual.toString()); atual.setLength(0);
            }
            for (int i = 0; i < bloco.length(); i += 1200) {
                if (atual.length() > 0) atual.append("\n\n");
                atual.append(bloco, i, Math.min(i + 1200, bloco.length()));
                if (atual.length() >= 1200) { saida.add(atual.toString()); atual.setLength(0); }
            }
        }
        if (atual.length() > 0) saida.add(atual.toString());
        return saida.size() > 1 ? List.copyOf(saida) : List.of();
    }
}
