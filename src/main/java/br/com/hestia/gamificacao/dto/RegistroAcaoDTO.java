package br.com.hestia.gamificacao.dto;
import br.com.hestia.gamificacao.model.AcaoExperiencia;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
public record RegistroAcaoDTO(@NotNull Long usuarioId, @NotNull AcaoExperiencia acao, @NotBlank String referencia) {}
