package br.com.hestia.reutilizacao.controller;

import br.com.hestia.reutilizacao.dto.RespostaReutilizavelDTO;
import br.com.hestia.reutilizacao.model.RespostaReutilizavel;
import br.com.hestia.reutilizacao.service.RespostaReutilizavelService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/respostas-reutilizaveis")
public class RespostaReutilizavelController {

    private final RespostaReutilizavelService service;

    public RespostaReutilizavelController(
            RespostaReutilizavelService service
    ) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RespostaReutilizavel cadastrar(
            @Valid @RequestBody RespostaReutilizavelDTO dto
    ) {
        return service.cadastrar(dto);
    }

    @GetMapping
    public List<RespostaReutilizavel> listarTodas() {
        return service.listarTodas();
    }

    @GetMapping("/{id}")
    public RespostaReutilizavel buscarPorId(
            @PathVariable Long id
    ) {
        return service.buscarPorId(id);
    }

    @GetMapping("/empresa/{empresaId}")
    public List<RespostaReutilizavel> listarPorEmpresa(
            @PathVariable Long empresaId
    ) {
        return service.listarPorEmpresa(empresaId);
    }

    @GetMapping("/empresa/{empresaId}/reutilizaveis")
    public List<RespostaReutilizavel> listarReutilizaveis(
            @PathVariable Long empresaId
    ) {
        return service.listarReutilizaveisPorEmpresa(empresaId);
    }

    @PatchMapping("/{id}/reutilizar")
    public RespostaReutilizavel registrarReutilizacao(
            @PathVariable Long id
    ) {
        return service.registrarReutilizacao(id);
    }
}