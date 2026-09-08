package br.com.hestia.microtreinamento.controller;

import br.com.hestia.microtreinamento.dto.ConclusaoMicrotreinamentoDTO;
import br.com.hestia.microtreinamento.model.ConclusaoMicrotreinamento;
import br.com.hestia.microtreinamento.service.ConclusaoMicrotreinamentoService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/microtreinamentos/conclusoes")
public class ConclusaoMicrotreinamentoController {

    private final ConclusaoMicrotreinamentoService service;

    public ConclusaoMicrotreinamentoController(
            ConclusaoMicrotreinamentoService service
    ) {
        this.service = service;
    }

    @PostMapping
    public ConclusaoMicrotreinamento registrarConclusao(
            @RequestBody ConclusaoMicrotreinamentoDTO dto
    ) {
        return service.registrarConclusao(
                dto.getUsuarioId(),
                dto.getMicrotreinamentoId()
        );
    }

    @GetMapping
    public List<ConclusaoMicrotreinamento> listarConclusoes() {
        return service.listarConclusoes();
    }
}