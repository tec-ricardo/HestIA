package br.com.hestia.rag.controller;
import br.com.hestia.rag.service.InteracaoRagService;
import br.com.hestia.usuario.model.Usuario;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/rag")
public class InteracaoRagController {
    private final InteracaoRagService service;
    public InteracaoRagController(InteracaoRagService service){this.service=service;}
    @PostMapping("/perguntas") public Object perguntar(@RequestAttribute("usuarioAutenticado")Usuario u,@RequestBody InteracaoRagService.Pergunta dto){return service.perguntar(u,dto);}
    @GetMapping("/historico") public Object historico(@RequestAttribute("usuarioAutenticado")Usuario u){return service.historico(u);}
}
