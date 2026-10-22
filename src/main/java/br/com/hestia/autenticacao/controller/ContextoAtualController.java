package br.com.hestia.autenticacao.controller;

import br.com.hestia.usuario.model.Usuario;
import br.com.hestia.usuario.model.PerfilUsuario;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;

@RestController
public class ContextoAtualController {
    public record Contexto(Long usuarioId,Long empresaId,Long departamentoId,PerfilUsuario perfil){}
    @GetMapping("/api/contexto")
    public ResponseEntity<Contexto> consultar(@RequestAttribute("usuarioAutenticado")Usuario usuario){
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(new Contexto(usuario.getId(),
            usuario.getEmpresa().getId(),usuario.getDepartamento()==null?null:usuario.getDepartamento().getId(),usuario.getPerfil()));
    }
}
