package br.com.hestia.microtreinamento.controller;

import br.com.hestia.microtreinamento.model.TipoErro;
import br.com.hestia.microtreinamento.service.EvolucaoMicrotreinamentoService;
import br.com.hestia.usuario.model.Usuario;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/microtreinamentos/conclusoes")
public class EvolucaoMicrotreinamentoController {

    private final EvolucaoMicrotreinamentoService service;

    public EvolucaoMicrotreinamentoController(EvolucaoMicrotreinamentoService service) {
        this.service = service;
    }

    @GetMapping("/{conclusaoId}/evolucao")
    public EvolucaoMicrotreinamentoService.Resultado consultar(
            @RequestAttribute("usuarioAutenticado") Usuario usuario,
            @PathVariable Long conclusaoId,
            @RequestParam TipoErro tipoErro
    ) {
        return service.consultar(usuario, conclusaoId, tipoErro);
    }
}
