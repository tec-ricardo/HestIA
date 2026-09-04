package br.com.hestia.empresa.controller;

import br.com.hestia.empresa.dto.EmpresaDTO;
import br.com.hestia.empresa.dto.EmpresaResponseDTO;
import br.com.hestia.empresa.service.EmpresaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/empresas")
public class EmpresaController {

    private final EmpresaService empresaService;

    public EmpresaController(EmpresaService empresaService) {
        this.empresaService = empresaService;
    }

    @PostMapping
    public ResponseEntity<EmpresaResponseDTO> cadastrar(
            @Valid @RequestBody EmpresaDTO dto) {

        var empresa = empresaService.cadastrar(dto);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(EmpresaResponseDTO.from(empresa));
    }

    @GetMapping
    public ResponseEntity<List<EmpresaResponseDTO>> listarTodas() {
        return ResponseEntity.ok(
                empresaService.listarTodas().stream().map(EmpresaResponseDTO::from).toList()
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<EmpresaResponseDTO> buscarPorId(
            @PathVariable Long id) {

        return ResponseEntity.ok(EmpresaResponseDTO.from(empresaService.buscarPorId(id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<EmpresaResponseDTO> atualizar(
            @PathVariable Long id,
            @Valid @RequestBody EmpresaDTO dto) {

        return ResponseEntity.ok(EmpresaResponseDTO.from(empresaService.atualizar(id, dto)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(
            @PathVariable Long id) {

        empresaService.excluir(id);

        return ResponseEntity.noContent().build();
    }
}
