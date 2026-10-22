package br.com.hestia.trilha.controller;
import br.com.hestia.trilha.service.MotorTrilhasService;
import br.com.hestia.usuario.model.Usuario;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
@RestController @RequestMapping("/trilhas")
public class MotorTrilhasController {
    private final MotorTrilhasService motor;
    public MotorTrilhasController(MotorTrilhasService motor){this.motor=motor;}
    @PostMapping("/ocorrencias-confirmadas") public Object confirmar(@RequestAttribute("usuarioAutenticado")Usuario u,@RequestBody MotorTrilhasService.Confirmacao dto){return Map.of("id",motor.confirmar(u,dto));}
    @PostMapping("/usuarios/{id}/processar") public Object processar(@RequestAttribute("usuarioAutenticado")Usuario u,@PathVariable Long id){motor.processar(u,id);return motor.ciclos(u,id);}
    @GetMapping("/usuarios/{id}/ciclos") public Object ciclos(@RequestAttribute("usuarioAutenticado")Usuario u,@PathVariable Long id){return motor.ciclos(u,id);}
    @GetMapping("/usuarios/{id}/pendencias") public Object pendencias(@RequestAttribute("usuarioAutenticado")Usuario u,@PathVariable Long id){return motor.pendencias(u,id);}
}
