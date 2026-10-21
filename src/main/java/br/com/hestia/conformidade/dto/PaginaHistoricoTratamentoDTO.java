package br.com.hestia.conformidade.dto;

import org.springframework.data.domain.Page;
import java.util.List;

public record PaginaHistoricoTratamentoDTO(List<EventoTratamentoDTO> itens,
        int pagina, int tamanho, long totalItens, int totalPaginas) {
    public PaginaHistoricoTratamentoDTO { itens = List.copyOf(itens); }
    public static PaginaHistoricoTratamentoDTO from(Page<EventoTratamentoDTO> p) {
        return new PaginaHistoricoTratamentoDTO(p.getContent(), p.getNumber(),
                p.getSize(), p.getTotalElements(), p.getTotalPages());
    }
}
