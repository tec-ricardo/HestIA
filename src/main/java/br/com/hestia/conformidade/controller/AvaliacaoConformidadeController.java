package br.com.hestia.conformidade.controller;

import br.com.hestia.conformidade.dto.AvaliacaoConformidadeDTO;
import br.com.hestia.conformidade.dto.AvaliacaoConformidadeResponseDTO;
import br.com.hestia.conformidade.service.AvaliacaoConformidadeService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/avaliacoes-conformidade")
public class AvaliacaoConformidadeController {
    private final AvaliacaoConformidadeService service;

    public AvaliacaoConformidadeController(AvaliacaoConformidadeService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AvaliacaoConformidadeResponseDTO avaliar(@Valid @RequestBody AvaliacaoConformidadeDTO dto) {
        return service.avaliar(dto);
    }

    @GetMapping("/{id}")
    public AvaliacaoConformidadeResponseDTO buscar(@PathVariable Long id) {
        return service.buscar(id);
    }
}
