package br.com.hestia.politica.controller;

import br.com.hestia.politica.dto.PoliticaUsoDTO;
import br.com.hestia.politica.model.PoliticaUso;
import br.com.hestia.politica.service.PoliticaUsoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/politicas")
public class PoliticaUsoController {

    private final PoliticaUsoService politicaUsoService;

    public PoliticaUsoController(PoliticaUsoService politicaUsoService) {
        this.politicaUsoService = politicaUsoService;
    }

    @PostMapping
    public ResponseEntity<PoliticaUso> criar(
            @Valid @RequestBody PoliticaUsoDTO dto
    ) {

        PoliticaUso politica = politicaUsoService.criar(dto);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(politica);
    }

    @GetMapping
    public ResponseEntity<List<PoliticaUso>> listarTodas() {

        return ResponseEntity.ok(
                politicaUsoService.listarTodas()
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<PoliticaUso> buscarPorId(
            @PathVariable Long id
    ) {

        return ResponseEntity.ok(
                politicaUsoService.buscarPorId(id)
        );
    }

    @GetMapping("/empresa/{empresaId}")
    public ResponseEntity<List<PoliticaUso>> listarPorEmpresa(
            @PathVariable Long empresaId
    ) {

        return ResponseEntity.ok(
                politicaUsoService.listarPorEmpresa(empresaId)
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<PoliticaUso> atualizar(
            @PathVariable Long id,
            @Valid @RequestBody PoliticaUsoDTO dto
    ) {

        return ResponseEntity.ok(
                politicaUsoService.atualizar(id, dto)
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(
            @PathVariable Long id
    ) {

        politicaUsoService.excluir(id);

        return ResponseEntity.noContent().build();
    }
}