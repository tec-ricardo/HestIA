package br.com.hestia.indicadores.controller;

import br.com.hestia.indicadores.dto.ComposicaoMaturidadeDTO;
import br.com.hestia.indicadores.service.ComposicaoMaturidadeService;
import br.com.hestia.reutilizavel.service.AcessoRespostaService;
import br.com.hestia.usuario.model.Usuario;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/indicadores/maturidade")
public class ComposicaoMaturidadeController {

    private final ComposicaoMaturidadeService service;
    private final AcessoRespostaService acesso;

    public ComposicaoMaturidadeController(
            ComposicaoMaturidadeService service,
            AcessoRespostaService acesso
    ) {
        this.service = service;
        this.acesso = acesso;
    }

    @GetMapping("/composicao")
    public ResponseEntity<ComposicaoMaturidadeDTO> consultar(
            @RequestAttribute("usuarioAutenticado") Usuario usuario,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate inicio,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fim,
            @RequestParam(required = false) String versao
    ) {
        acesso.validarEmpresa(usuario, null);

        return ResponseEntity.ok(
                service.consultar(inicio, fim, versao)
        );
    }
}