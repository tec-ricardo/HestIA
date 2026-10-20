package br.com.hestia.autenticacao.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record RecuperacaoSenhaDTO(@NotBlank @Email String email) {}