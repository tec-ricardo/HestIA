package br.com.hestia.indicadores.controller;
import br.com.hestia.indicadores.service.QualidadeDadosService;
import br.com.hestia.usuario.model.Usuario;
import org.springframework.web.bind.annotation.*;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;
@RestController @RequestMapping("/indicadores/dados")
public class QualidadeDadosController {
    private final QualidadeDadosService service;
    public QualidadeDadosController(QualidadeDadosService service){this.service=service;}
    @GetMapping("/validacao") public QualidadeDadosService.Resultado validar(@RequestAttribute("usuarioAutenticado")Usuario u,
        @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE)LocalDate inicio,
        @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE)LocalDate fim,
        @RequestParam int minimoRegistros){
        var resultado=service.consultar(u,inicio,fim,minimoRegistros);
        service.exigirDadosMinimos(resultado);
        return resultado;
    }
    @GetMapping("/qualidade") public QualidadeDadosService.Resultado consultar(@RequestAttribute("usuarioAutenticado")Usuario u,
        @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE)LocalDate inicio,
        @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE)LocalDate fim,
        @RequestParam int minimoRegistros){return service.consultar(u,inicio,fim,minimoRegistros);}
}
