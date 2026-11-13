package br.com.hestia.auditoria.controller;

import br.com.hestia.auditoria.dto.LogAuditoriaResponseDTO;
import br.com.hestia.auditoria.service.AuditoriaService;
import br.com.hestia.usuario.model.PerfilUsuario;
import br.com.hestia.usuario.model.Usuario;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/auditoria")
public class AuditoriaController {
    private final AuditoriaService service;
    public AuditoriaController(AuditoriaService service) { this.service = service; }

    @GetMapping
    public List<LogAuditoriaResponseDTO> listar(
            @RequestAttribute("usuarioAutenticado") Usuario usuario,
            @RequestParam(required = false) Long empresaId,
            @RequestParam(required = false) Long usuarioId,
            @RequestParam(required = false) String acao,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fim) {
        if (usuario.getPerfil() != PerfilUsuario.ADMIN
                || usuario.getEmpresa() == null || usuario.getEmpresa().getId() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso à auditoria indisponível.");
        }
        Long empresaAutenticada = usuario.getEmpresa().getId();
        if (empresaId != null && !empresaAutenticada.equals(empresaId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Empresa não autorizada.");
        }
        return service.listar(empresaAutenticada, usuarioId, acao, inicio, fim);
    }
}
