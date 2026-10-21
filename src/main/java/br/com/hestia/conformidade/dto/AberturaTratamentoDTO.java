package br.com.hestia.conformidade.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AberturaTratamentoDTO(@NotNull @Positive Long avaliacaoId) {}
