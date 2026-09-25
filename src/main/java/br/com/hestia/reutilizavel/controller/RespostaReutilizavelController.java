package br.com.hestia.reutilizavel.controller;

import br.com.hestia.reutilizavel.dto.EconomiaReutilizacaoDTO;
import br.com.hestia.reutilizavel.dto.RespostaReutilizavelDTO;
import br.com.hestia.reutilizavel.dto.SustentabilidadeDTO;
import br.com.hestia.reutilizavel.model.RespostaReutilizavel;
import br.com.hestia.reutilizavel.service.RespostaReutilizavelService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<RespostaReutilizavel> cadastrar(
            @Valid @RequestBody RespostaReutilizavelDTO dto
    ) {

        var resposta = service.cadastrar(dto);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(resposta);
    }

    @GetMapping
    public ResponseEntity<List<RespostaReutilizavel>>
    listarTodas() {

        return ResponseEntity.ok(
                service.listarTodas()
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<RespostaReutilizavel>
    buscarPorId(
            @PathVariable Long id
    ) {

        return ResponseEntity.ok(
                service.buscarPorId(id)
        );
    }

    @GetMapping("/empresa/{empresaId}")
    public ResponseEntity<List<RespostaReutilizavel>>
    listarPorEmpresa(
            @PathVariable Long empresaId
    ) {

        return ResponseEntity.ok(
                service.listarPorEmpresa(empresaId)
        );
    }

    @GetMapping("/empresa/{empresaId}/reutilizaveis")
    public ResponseEntity<List<RespostaReutilizavel>>
    listarReutilizaveis(
            @PathVariable Long empresaId
    ) {

        return ResponseEntity.ok(
                service.listarReutilizaveisPorEmpresa(
                        empresaId
                )
        );
    }

    @GetMapping("/empresa/{empresaId}/buscar")
    public ResponseEntity<List<RespostaReutilizavel>>
    buscarRespostasAnteriores(
            @PathVariable Long empresaId,
            @RequestParam String prompt
    ) {

        return ResponseEntity.ok(
                service.buscarRespostasAnteriores(
                        empresaId,
                        prompt
                )
        );
    }

    @PatchMapping("/{id}/reutilizar")
    public ResponseEntity<RespostaReutilizavel>
    registrarReutilizacao(
            @PathVariable Long id
    ) {

        return ResponseEntity.ok(
                service.registrarReutilizacao(id)
        );
    }

    @GetMapping("/empresa/{empresaId}/economia")
    public ResponseEntity<EconomiaReutilizacaoDTO>
    consultarEconomia(
            @PathVariable Long empresaId
    ) {

        return ResponseEntity.ok(
                service.consultarEconomia(empresaId)
        );
    }

    @GetMapping("/empresa/{empresaId}/sustentabilidade")
    public ResponseEntity<SustentabilidadeDTO>
    consultarSustentabilidade(
            @PathVariable Long empresaId
    ) {

        return ResponseEntity.ok(
                service.consultarSustentabilidade(
                        empresaId
                )
        );
    }
}