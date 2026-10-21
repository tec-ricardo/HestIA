package br.com.hestia.conformidade.controller;

import br.com.hestia.conformidade.dto.*;
import br.com.hestia.conformidade.model.StatusTratamento;
import br.com.hestia.conformidade.service.TratamentoNaoConformidadeService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.net.URI;

@RestController
@RequestMapping("/tratamentos-nao-conformidade")
public class TratamentoNaoConformidadeController {
    private final TratamentoNaoConformidadeService service;

    public TratamentoNaoConformidadeController(TratamentoNaoConformidadeService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<TratamentoDetalheDTO> abrir(@Valid @RequestBody AberturaTratamentoDTO dto) {
        var t = service.abrir(dto);
        return ResponseEntity.created(URI.create("/tratamentos-nao-conformidade/" + t.tratamento().id())).body(t);
    }

    @PutMapping("/{id}")
    public TratamentoDetalheDTO atualizar(@PathVariable Long id,
            @Valid @RequestBody AtualizacaoTratamentoDTO dto) {
        return service.atualizar(id, dto);
    }

    @GetMapping("/{id}")
    public TratamentoDetalheDTO buscar(@PathVariable Long id) {
        return service.buscar(id);
    }

    @GetMapping
    public PaginaTratamentosDTO listar(@RequestParam(required = false) StatusTratamento status,
            @RequestParam(defaultValue = "0") int pagina, @RequestParam(defaultValue = "20") int tamanho) {
        return service.listar(status, pagina, tamanho);
    }
}
