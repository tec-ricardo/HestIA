package br.com.hestia.indicadores.controller;

import br.com.hestia.indicadores.dto.IndicadoresConfiancaDTO;
import br.com.hestia.indicadores.service.ConsultaConfiancaIndicadoresService;
import br.com.hestia.indicadores.service.ConsultaConfiancaIndicadoresService.Visao;
import br.com.hestia.usuario.model.Usuario;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;

@RestController
@RequestMapping("/indicadores/confiabilidade")
public class ConfiancaIndicadoresController {
    private final ConsultaConfiancaIndicadoresService service;
    public ConfiancaIndicadoresController(ConsultaConfiancaIndicadoresService service) { this.service = service; }

    @GetMapping
    public ResponseEntity<IndicadoresConfiancaDTO> consultar(
            @RequestAttribute("usuarioAutenticado") Usuario usuario,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
            @RequestParam(defaultValue = "PESSOAL") Visao visao,
            @RequestParam(required = false) String versao) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(service.consultar(usuario, inicio, fim, visao, versao));
    }
}
