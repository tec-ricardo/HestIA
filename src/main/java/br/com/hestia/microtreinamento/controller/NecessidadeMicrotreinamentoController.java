package br.com.hestia.microtreinamento.controller;

import br.com.hestia.microtreinamento.model.NecessidadeMicrotreinamento;
import br.com.hestia.microtreinamento.model.TipoErro;
import br.com.hestia.microtreinamento.service.NecessidadeMicrotreinamentoService;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/microtreinamentos/necessidades")
public class NecessidadeMicrotreinamentoController {

    private final NecessidadeMicrotreinamentoService service;

    public NecessidadeMicrotreinamentoController(
            NecessidadeMicrotreinamentoService service) {
        this.service = service;
    }

    @PostMapping
    public NecessidadeMicrotreinamento identificar(
            @RequestParam Long usuarioId,
            @RequestParam TipoErro tipoErro,
            @RequestBody List<LocalDateTime> ocorrencias) {

        return service.identificarNecessidade(
                usuarioId,
                tipoErro,
                ocorrencias
        );
    }

    @GetMapping
    public List<NecessidadeMicrotreinamento> listar() {
        return service.listarTodas();
    }
}