package br.com.hestia.microtreinamento.controller;

import br.com.hestia.microtreinamento.model.TipoErro;
import br.com.hestia.microtreinamento.service.ConsultaEfetividadeCapacitacaoService;
import br.com.hestia.usuario.model.Usuario;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/microtreinamentos/conclusoes")
public class EfetividadeCapacitacaoController {

    private final ConsultaEfetividadeCapacitacaoService service;

    public EfetividadeCapacitacaoController(
            ConsultaEfetividadeCapacitacaoService service
    ) {
        this.service = service;
    }

    @GetMapping("/{conclusaoId}/efetividade")
    public ConsultaEfetividadeCapacitacaoService.Resultado consultar(
            @RequestAttribute("usuarioAutenticado") Usuario usuario,
            @PathVariable Long conclusaoId,
            @RequestParam TipoErro tipoErro
    ) {
        return service.consultar(
                usuario,
                conclusaoId,
                tipoErro
        );
    }
}