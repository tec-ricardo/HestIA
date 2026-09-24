package br.com.hestia.auditoria.controller;

import br.com.hestia.auditoria.dto.LogAuditoriaCriacaoDTO;
import br.com.hestia.auditoria.dto.LogAuditoriaResponseDTO;
import br.com.hestia.auditoria.model.TipoAcaoAuditoria;
import br.com.hestia.auditoria.service.LogAuditoriaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/auditoria")
public class LogAuditoriaController {

    private final LogAuditoriaService logAuditoriaService;

    public LogAuditoriaController(
            LogAuditoriaService logAuditoriaService
    ) {
        this.logAuditoriaService = logAuditoriaService;
    }

    @PostMapping
    public ResponseEntity<LogAuditoriaResponseDTO> registrar(
            @Valid @RequestBody LogAuditoriaCriacaoDTO dto
    ) {

        var log = logAuditoriaService.registrar(dto);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(LogAuditoriaResponseDTO.from(log));
    }

    @GetMapping
    public ResponseEntity<List<LogAuditoriaResponseDTO>>
    listarTodos() {

        var logs = logAuditoriaService
                .listarTodos()
                .stream()
                .map(LogAuditoriaResponseDTO::from)
                .toList();

        return ResponseEntity.ok(logs);
    }

    @GetMapping("/{id}")
    public ResponseEntity<LogAuditoriaResponseDTO>
    buscarPorId(
            @PathVariable Long id
    ) {

        return ResponseEntity.ok(
                LogAuditoriaResponseDTO.from(
                        logAuditoriaService.buscarPorId(id)
                )
        );
    }

    @GetMapping("/usuario/{usuarioId}")
    public ResponseEntity<List<LogAuditoriaResponseDTO>>
    buscarPorUsuario(
            @PathVariable Long usuarioId
    ) {

        var logs = logAuditoriaService
                .buscarPorUsuario(usuarioId)
                .stream()
                .map(LogAuditoriaResponseDTO::from)
                .toList();

        return ResponseEntity.ok(logs);
    }

    @GetMapping("/entidade/{entidade}")
    public ResponseEntity<List<LogAuditoriaResponseDTO>>
    buscarPorEntidade(
            @PathVariable String entidade
    ) {

        var logs = logAuditoriaService
                .buscarPorEntidade(entidade)
                .stream()
                .map(LogAuditoriaResponseDTO::from)
                .toList();

        return ResponseEntity.ok(logs);
    }

    @GetMapping("/acao/{acao}")
    public ResponseEntity<List<LogAuditoriaResponseDTO>>
    buscarPorAcao(
            @PathVariable TipoAcaoAuditoria acao
    ) {

        var logs = logAuditoriaService
                .buscarPorAcao(acao)
                .stream()
                .map(LogAuditoriaResponseDTO::from)
                .toList();

        return ResponseEntity.ok(logs);
    }
}