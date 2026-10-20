package br.com.hestia.rag.controller;

import br.com.hestia.rag.dto.ReindexacaoDTO;
import br.com.hestia.rag.dto.ResultadoReindexacaoDTO;
import br.com.hestia.rag.service.ReindexacaoService;
import br.com.hestia.usuario.model.Usuario;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/respostas-reutilizaveis")
public class ReindexacaoController {

    private final ReindexacaoService service;

    public ReindexacaoController(ReindexacaoService service) {
        this.service = service;
    }

    @PostMapping("/reindexar")
    public ResponseEntity<List<ResultadoReindexacaoDTO>> reindexar(
            @RequestAttribute("usuarioAutenticado") Usuario usuario,
            @Valid @RequestBody ReindexacaoDTO dto
    ) {
        return ResponseEntity.ok(
                service.reindexar(usuario, dto.respostas())
        );
    }
}