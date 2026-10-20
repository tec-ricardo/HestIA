package br.com.hestia.indicadores.controller;

import br.com.hestia.indicadores.dto.ComposicaoMaturidadeDTO;
import br.com.hestia.indicadores.service.ComposicaoMaturidadeService;
import br.com.hestia.reutilizavel.service.AcessoRespostaService;
import br.com.hestia.usuario.model.Usuario;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
            @RequestAttribute("usuarioAutenticado") Usuario usuario
    ) {
        acesso.validarEmpresa(usuario, null);
        return ResponseEntity.ok(service.consultar());
    }
}