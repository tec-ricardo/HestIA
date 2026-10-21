package br.com.hestia.reutilizavel.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RetiradaRespostaDTO(@NotBlank @Size(max = 1000) String justificativa) {}
