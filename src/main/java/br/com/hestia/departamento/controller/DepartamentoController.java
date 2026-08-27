package br.com.hestia.departamento.controller;

import br.com.hestia.departamento.dto.DepartamentoDTO;
import br.com.hestia.departamento.model.Departamento;
import br.com.hestia.departamento.service.DepartamentoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/departamentos")
public class DepartamentoController {

    private final DepartamentoService departamentoService;

    public DepartamentoController(
            DepartamentoService departamentoService) {

        this.departamentoService = departamentoService;
    }

    @PostMapping
    public ResponseEntity<Departamento> cadastrar(
            @Valid @RequestBody DepartamentoDTO dto) {

        Departamento departamento =
                departamentoService.cadastrar(dto);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(departamento);
    }

    @GetMapping
    public ResponseEntity<List<Departamento>> listarTodos() {

        return ResponseEntity.ok(
                departamentoService.listarTodos()
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<Departamento> buscarPorId(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                departamentoService.buscarPorId(id)
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<Departamento> atualizar(
            @PathVariable Long id,
            @Valid @RequestBody DepartamentoDTO dto) {

        return ResponseEntity.ok(
                departamentoService.atualizar(id, dto)
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(
            @PathVariable Long id) {

        departamentoService.excluir(id);

        return ResponseEntity.noContent().build();
    }
}