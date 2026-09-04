package br.com.hestia.ferramenta.controller;

import br.com.hestia.ferramenta.dto.FerramentaIACriacaoDTO;
import br.com.hestia.ferramenta.dto.FerramentaIAResponseDTO;
import br.com.hestia.ferramenta.service.FerramentaIAService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/ferramentas-ia")
public class FerramentaIAController {

    private final FerramentaIAService ferramentaService;

    public FerramentaIAController(FerramentaIAService ferramentaService) {
        this.ferramentaService = ferramentaService;
    }

    @PostMapping
    public ResponseEntity<FerramentaIAResponseDTO> criar(
            @Valid @RequestBody FerramentaIACriacaoDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(FerramentaIAResponseDTO.from(ferramentaService.criar(dto)));
    }

    @GetMapping
    public List<FerramentaIAResponseDTO> listar(
            @RequestParam(required = false) Long empresaId) {
        return ferramentaService.listar(empresaId).stream()
                .map(FerramentaIAResponseDTO::from)
                .toList();
    }

    @GetMapping("/{id}")
    public FerramentaIAResponseDTO buscar(@PathVariable Long id) {
        return FerramentaIAResponseDTO.from(ferramentaService.buscar(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        ferramentaService.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
