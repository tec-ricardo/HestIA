package br.com.hestia.auditoria.dto;

import br.com.hestia.auditoria.model.ResultadoAuditoria;
import br.com.hestia.auditoria.model.TipoAcaoAuditoria;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record LogAuditoriaCriacaoDTO(

        Long usuarioId,

        @NotNull(message = "A ação é obrigatória.")
        TipoAcaoAuditoria acao,

        @NotBlank(message = "A entidade é obrigatória.")
        @Size(max = 100, message = "A entidade deve possuir no máximo 100 caracteres.")
        String entidade,

        Long entidadeId,

        @Size(max = 1000, message = "A descrição deve possuir no máximo 1000 caracteres.")
        String descricao,

        @NotNull(message = "O resultado é obrigatório.")
        ResultadoAuditoria resultado,

        String dadosAnteriores,

        String dadosNovos
) {
}