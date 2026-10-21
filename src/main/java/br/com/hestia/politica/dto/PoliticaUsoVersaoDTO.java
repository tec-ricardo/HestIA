package br.com.hestia.politica.dto;
import br.com.hestia.politica.model.PoliticaUsoVersao;
import java.time.LocalDateTime;

public record PoliticaUsoVersaoDTO(Long id, Long politicaUsoId, Long empresaId,
        int revisao, String versao, String titulo, String descricao, String conteudo,
        boolean ativa, boolean excluida, String origem, LocalDateTime registradaEm, Long autorId) {
    public static PoliticaUsoVersaoDTO from(PoliticaUsoVersao v) {
        return new PoliticaUsoVersaoDTO(v.getId(), v.getPoliticaUsoId(), v.getEmpresaId(),
                v.getRevisao(), v.getVersao(), v.getTitulo(), v.getDescricao(), v.getConteudo(),
                v.isAtiva(), v.isExcluida(), v.getOrigem(), v.getRegistradaEm(), v.getAutorId());
    }
}
