package br.com.hestia.usuario.dto;

import br.com.hestia.usuario.model.PerfilUsuario;
import br.com.hestia.usuario.model.Usuario;

import java.time.LocalDateTime;

public record UsuarioResponseDTO(
        Long id,
        String nome,
        String email,
        String cargo,
        PerfilUsuario perfil,
        Boolean ativo,
        LocalDateTime dataCadastro,
        Long empresaId,
        Long departamentoId
) {
    public static UsuarioResponseDTO from(Usuario usuario) {
        return new UsuarioResponseDTO(
                usuario.getId(), usuario.getNome(), usuario.getEmail(), usuario.getCargo(),
                usuario.getPerfil(), usuario.getAtivo(), usuario.getDataCadastro(),
                usuario.getEmpresa().getId(), usuario.getDepartamento().getId()
        );
    }
}
