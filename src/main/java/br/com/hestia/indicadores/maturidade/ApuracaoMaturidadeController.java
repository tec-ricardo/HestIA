package br.com.hestia.indicadores.maturidade;
import br.com.hestia.usuario.model.Usuario;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
@RestController @RequestMapping("/indicadores/maturidade")
public class ApuracaoMaturidadeController {
    private final ApuracaoMaturidadeService service;
    public ApuracaoMaturidadeController(ApuracaoMaturidadeService service){this.service=service;}
    @PostMapping("/consulta-consolidada") public Object consultar(@RequestAttribute("usuarioAutenticado")Usuario u,@RequestBody ResultadoMaturidadeService.Consulta dto){return service.consultar(u,dto);}
    @PostMapping("/apuracoes") public Object registrar(@RequestAttribute("usuarioAutenticado")Usuario u,@RequestBody ResultadoMaturidadeService.Consulta dto){return Map.of("id",service.registrar(u,dto),"natureza","REGISTRO_EXPERIMENTAL_NAO_HOMOLOGADO");}
    @GetMapping("/apuracoes") public Object listar(@RequestAttribute("usuarioAutenticado")Usuario u){return service.historico(u);}
    @GetMapping("/apuracoes/{id}") public Object obter(@RequestAttribute("usuarioAutenticado")Usuario u,@PathVariable Long id){return service.obter(u,id);}
    @GetMapping("/evolucao") public Object evolucao(@RequestAttribute("usuarioAutenticado")Usuario u,@RequestParam Long anteriorId,@RequestParam Long atualId){return service.evolucao(u,anteriorId,atualId);}
}
