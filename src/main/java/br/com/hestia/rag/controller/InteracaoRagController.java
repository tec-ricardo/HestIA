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
    @DeleteMapping("/historico") public void apagarHistorico(@RequestAttribute("usuarioAutenticado") Usuario u){service.apagarHistorico(u);}
    @DeleteMapping("/historico/{id}") public void apagarInteracao(@RequestAttribute("usuarioAutenticado") Usuario u,@PathVariable Long id){service.apagarInteracao(u,id);}
    @GetMapping("/conversas/{id}/contexto") public Object contexto(@RequestAttribute("usuarioAutenticado")Usuario u,@PathVariable Long id){return service.limitesContexto(u,id);}
    public record NovaConversa(String titulo){}
    @PostMapping("/conversas") public Object criarConversa(@RequestAttribute("usuarioAutenticado")Usuario u,@RequestBody NovaConversa dto){return service.criarConversa(u,dto.titulo());}
    @GetMapping("/conversas") public Object conversas(@RequestAttribute("usuarioAutenticado")Usuario u){return service.conversas(u);}
    @GetMapping("/conversas/{id}/historico") public Object historicoConversa(@RequestAttribute("usuarioAutenticado")Usuario u,@PathVariable Long id){return service.historicoConversa(u,id);}
}
