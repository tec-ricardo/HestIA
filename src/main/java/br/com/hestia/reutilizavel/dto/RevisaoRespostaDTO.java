package br.com.hestia.reutilizavel.dto;

import java.time.LocalDateTime;
import java.util.List;
import br.com.hestia.rag.model.RespostaVersao;
import br.com.hestia.reutilizavel.model.RespostaReutilizavel;

public record RevisaoRespostaDTO(Long id, Long empresaId, Long autorConteudoId, LocalDateTime criadaEm,
        String categoria, String modeloIA, String promptOriginal, String respostaGerada,
        boolean reutilizavel, Long retiradaPorId, LocalDateTime retiradaEm, String justificativaRetirada,
        boolean retiradaDocumentada, List<Versao> versoes) {
    public RevisaoRespostaDTO { versoes = List.copyOf(versoes); }
    public record Versao(int numero, String origem, String tipoConteudo, LocalDateTime criadaEm,
            String promptOriginal, String respostaGerada) {}
    public static RevisaoRespostaDTO from(RespostaReutilizavel r, List<RespostaVersao> versoes) {
        return new RevisaoRespostaDTO(r.getId(), r.getEmpresaId(), r.getUsuarioId(), r.getDataCriacao(),
                r.getCategoria(), r.getModeloIA(), r.getPromptOriginal(), r.getRespostaGerada(),
                Boolean.TRUE.equals(r.getReutilizavel()), r.getRetiradaPorId(), r.getRetiradaEm(),
                r.getJustificativaRetirada(), r.getRetiradaEm() != null,
                versoes.stream().map(v -> new Versao(v.getNumeroVersao(), v.getOrigem(), v.getTipoConteudo(),
                        v.getCriadaEm(), v.getPromptOriginal(), v.getRespostaGerada())).toList());
    }
}
