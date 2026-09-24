package br.com.hestia.gamificacao.dto;
import br.com.hestia.gamificacao.model.NivelExperiencia;
import java.util.List;
public record PasseHestIAResponseDTO(Long usuarioId, String nome, int xpTotal, NivelExperiencia nivel,
                                    Integer xpProximoNivel, int percentualProgresso, List<MissaoDTO> missoes) {
    public record MissaoDTO(String codigo, String titulo, String descricao, int progresso, int meta,
                            int recompensaXp, boolean concluida) {}
}
