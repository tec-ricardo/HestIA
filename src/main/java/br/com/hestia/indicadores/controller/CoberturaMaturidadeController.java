package br.com.hestia.indicadores.controller;

import br.com.hestia.indicadores.dto.ConsultaCoberturaDTO;
import br.com.hestia.indicadores.maturidade.ResultadoMaturidadeService;
import br.com.hestia.indicadores.service.ConsultaCoberturaService;
import br.com.hestia.usuario.model.Usuario;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/indicadores/maturidade")
public class CoberturaMaturidadeController {

    private final ConsultaCoberturaService service;

    public CoberturaMaturidadeController(ConsultaCoberturaService service) {
        this.service = service;
    }

    @GetMapping("/cobertura")
    public ResponseEntity<ConsultaCoberturaDTO> consultar(
            @RequestAttribute("usuarioAutenticado") Usuario usuario,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
            @RequestParam(required = false) String versao,
            @RequestParam(defaultValue = "OPERACIONAL") ResultadoMaturidadeService.Natureza fonte,
            @RequestParam(required = false) String snapshot,
            @RequestParam(required = false) Long departamentoId,
            @RequestParam(required = false) String departamentoOrigem,
            @RequestParam(required = false) String tipoIssue
    ) {
        return ResponseEntity.ok(service.consultar(
                usuario, versao, new ResultadoMaturidadeService.Consulta(
                        inicio, fim, fonte, snapshot, departamentoId,
                        departamentoOrigem, tipoIssue
                )
        ));
    }
}
