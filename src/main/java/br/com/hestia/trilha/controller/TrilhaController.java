package br.com.hestia.trilha.controller;
import br.com.hestia.trilha.service.TrilhaService;
import br.com.hestia.usuario.model.Usuario;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import java.util.List;
@RestController @RequestMapping("/trilhas")
public class TrilhaController {
    private final TrilhaService service;
    public TrilhaController(TrilhaService service){this.service=service;}
    public record Criacao(String titulo,List<Long> microtreinamentos){}
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public TrilhaService.Definicao criar(@RequestAttribute("usuarioAutenticado")Usuario u,@RequestBody Criacao dto){return service.criar(u,dto.titulo(),dto.microtreinamentos());}
    @GetMapping public List<TrilhaService.Definicao> listar(@RequestAttribute("usuarioAutenticado")Usuario u){return service.listar(u);}
    @PostMapping("/{id}/usuarios/{usuarioId}")
    public TrilhaService.Progresso iniciar(@RequestAttribute("usuarioAutenticado")Usuario u,@PathVariable Long id,@PathVariable Long usuarioId){return service.iniciar(u,id,usuarioId);}
    @GetMapping("/usuarios/{usuarioId}")
    public List<TrilhaService.Progresso> historico(@RequestAttribute("usuarioAutenticado")Usuario u,@PathVariable Long usuarioId){return service.historico(u,usuarioId);}
    @GetMapping("/participacoes/{id}")
    public TrilhaService.Progresso consultar(@RequestAttribute("usuarioAutenticado")Usuario u,@PathVariable Long id){return service.consultar(u,id);}
    @GetMapping("/conclusoes/{id}")
    public List<TrilhaService.Progresso> porConclusao(@RequestAttribute("usuarioAutenticado")Usuario u,@PathVariable Long id){return service.porConclusao(u,id);}
}
