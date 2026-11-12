package br.com.hestia.registro.controller;
import br.com.hestia.reutilizavel.service.RespostaReutilizavelService;
import br.com.hestia.usuario.model.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;
@RestController @RequestMapping("/relatorios-economia")
public class RelatorioEconomiaController {
 private final RespostaReutilizavelService service;
 public RelatorioEconomiaController(RespostaReutilizavelService service){this.service=service;}
 @GetMapping({"","/exportar"}) public Object consultar(@RequestAttribute("usuarioAutenticado")Usuario u,@RequestParam Map<String,String> filtros,jakarta.servlet.http.HttpServletRequest request){
  if(u.getPerfil()!=PerfilUsuario.ADMIN)throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Acumulado empresarial restrito ao administrador");
  if(!filtros.isEmpty())throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"O cálculo existente é acumulado; não suporta filtros de período ou departamento");
  var e=service.consultarEconomia(u.getEmpresa().getId());
  var dados=new LinkedHashMap<String,Object>();dados.put("escopo","ACUMULADO_EMPRESA_SEM_FILTRO_TEMPORAL");dados.put("empresaId",e.empresaId());dados.put("chamadasEvitadas",e.chamadasEvitadas());dados.put("tokensEconomizados",e.tokensEconomizados());dados.put("custoEstimadoEvitado",e.custoEstimadoEvitado());dados.put("percentualReutilizacao",e.percentualReutilizacao());dados.put("limitacao","Economia monetária indisponível até preços, moeda e metodologia aprovados; não representa custo operacional medido.");
  if(!request.getRequestURI().endsWith("/exportar"))return dados;
  var csv=new StringBuilder("campo,valor\r\n");dados.forEach((k,v)->csv.append(RelatorioUsoController.campo(k)).append(',').append(RelatorioUsoController.campo(v)).append("\r\n"));
  return ResponseEntity.ok().cacheControl(CacheControl.noStore()).header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=economia-acumulada.csv").contentType(MediaType.parseMediaType("text/csv;charset=UTF-8")).body(csv.toString());
 }
}
