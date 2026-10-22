package br.com.hestia.reutilizavel.controller;
import br.com.hestia.reutilizavel.service.*;
import br.com.hestia.usuario.model.Usuario;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/admin/respostas")
public class RevisaoRespostaController {
    private final RevisaoRespostaService revisao;private final RespostaReutilizavelService respostas;
    public RevisaoRespostaController(RevisaoRespostaService revisao,RespostaReutilizavelService respostas){this.revisao=revisao;this.respostas=respostas;}
    @GetMapping public Object listar(@RequestAttribute("usuarioAutenticado")Usuario u,@RequestParam(defaultValue="0")int pagina,@RequestParam(defaultValue="50")int tamanho){return revisao.listar(u,pagina,tamanho);}
    @GetMapping("/{id}") public Object detalhe(@RequestAttribute("usuarioAutenticado")Usuario u,@PathVariable Long id){return revisao.detalhe(u,id);}
    @PostMapping("/{id}/revisoes") public Object revisar(@RequestAttribute("usuarioAutenticado")Usuario u,@PathVariable Long id,@RequestBody RevisaoRespostaService.Acao dto){return revisao.revisar(u,id,dto);}
}
