package br.com.hestia.indicadores.maturidade;

import br.com.hestia.usuario.model.Usuario;
import org.springframework.web.bind.annotation.*;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;

@RestController @RequestMapping("/indicadores/maturidade")
public class ResultadoMaturidadeController {
    private final ResultadoMaturidadeService service;
    public ResultadoMaturidadeController(ResultadoMaturidadeService service){this.service=service;}
    @GetMapping("/resultados")
    public ResultadoMaturidadeService.Resultado consultar(@RequestAttribute("usuarioAutenticado")Usuario usuario,
        @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE)LocalDate inicio,
        @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE)LocalDate fim,
        @RequestParam ResultadoMaturidadeService.Natureza fonte,@RequestParam(required=false)String snapshot,
        @RequestParam(required=false)Long departamentoId,@RequestParam(required=false)String departamentoOrigem,
        @RequestParam(required=false)String tipoIssue){
        return service.consultar(usuario,new ResultadoMaturidadeService.Consulta(inicio,fim,fonte,snapshot,departamentoId,departamentoOrigem,tipoIssue));
    }
}
