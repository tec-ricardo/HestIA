package br.com.hestia.conformidade.dto;

import org.springframework.data.domain.Page;
import java.util.List;

public record PaginaTratamentosDTO(List<TratamentoResumoDTO> itens,
        int pagina, int tamanho, long totalItens, int totalPaginas) {
    public static PaginaTratamentosDTO from(Page<TratamentoResumoDTO> p) {
        return new PaginaTratamentosDTO(p.getContent(), p.getNumber(),
                p.getSize(), p.getTotalElements(), p.getTotalPages());
    }
}
