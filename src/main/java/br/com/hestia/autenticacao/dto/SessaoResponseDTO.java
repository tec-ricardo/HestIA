package br.com.hestia.autenticacao.dto;

import br.com.hestia.usuario.model.PerfilUsuario;
import java.time.LocalDateTime;

public record SessaoResponseDTO(String token, LocalDateTime expiraEm, Long usuarioId,
                                String nome, PerfilUsuario perfil) {}
