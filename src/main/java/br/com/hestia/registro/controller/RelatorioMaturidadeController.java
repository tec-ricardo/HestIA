package br.com.hestia.registro.controller;
import br.com.hestia.indicadores.maturidade.*;
import br.com.hestia.usuario.model.Usuario;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import java.time.*;
import java.util.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

/** Exporta o resultado dos serviços existentes, mantendo filtros e autorização. */
@RestController @RequestMapping("/relatorios-maturidade")
public class RelatorioMaturidadeController {
 private final ResultadoMaturidadeService resultados; private final ApuracaoMaturidadeService apuracoes;
 private final ObjectMapper json;
 public RelatorioMaturidadeController(ResultadoMaturidadeService resultados,ApuracaoMaturidadeService apuracoes){
  this.resultados=resultados;this.apuracoes=apuracoes;json=new ObjectMapper();var m=new SimpleModule();
  m.addSerializer(LocalDate.class,ToStringSerializer.instance);m.addSerializer(LocalDateTime.class,ToStringSerializer.instance);m.addSerializer(Instant.class,ToStringSerializer.instance);json.registerModule(m);
 }
 @GetMapping({"/resultados","/resultados.csv"})
 public Object consultar(@RequestAttribute("usuarioAutenticado")Usuario usuario,
  @RequestParam LocalDate inicio,@RequestParam LocalDate fim,@RequestParam ResultadoMaturidadeService.Natureza fonte,
  @RequestParam(required=false)String snapshot,@RequestParam(required=false)Long departamentoId,
  @RequestParam(required=false)String departamentoOrigem,@RequestParam(required=false)String tipoIssue,
  jakarta.servlet.http.HttpServletRequest request){
  var resultado=resultados.consultar(usuario,new ResultadoMaturidadeService.Consulta(inicio,fim,fonte,snapshot,departamentoId,departamentoOrigem,tipoIssue));
  return request.getRequestURI().endsWith(".csv")?csv(resultado,"maturidade.csv"):resultado;
 }
 @GetMapping({"/evolucao","/evolucao.csv"})
 public Object evolucao(@RequestAttribute("usuarioAutenticado")Usuario usuario,@RequestParam Long anteriorId,@RequestParam Long atualId,jakarta.servlet.http.HttpServletRequest request){
  var resultado=apuracoes.evolucao(usuario,anteriorId,atualId);
  return request.getRequestURI().endsWith(".csv")?csv(resultado,"evolucao.csv"):resultado;
 }
 private ResponseEntity<String> csv(Object resultado,String arquivo){
  var texto=new StringBuilder("campo,valor\r\n");achatar(json.valueToTree(resultado),"",texto);
  return ResponseEntity.ok().cacheControl(CacheControl.noStore()).header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename="+arquivo).contentType(MediaType.parseMediaType("text/csv;charset=UTF-8")).body(texto.toString());
 }
 private void achatar(JsonNode node,String caminho,StringBuilder csv){
  if(node.isObject()){var campos=node.fields();while(campos.hasNext()){var c=campos.next();achatar(c.getValue(),caminho.isEmpty()?c.getKey():caminho+"."+c.getKey(),csv);}}
  else if(node.isArray()){for(int i=0;i<node.size();i++)achatar(node.get(i),caminho+"["+i+"]",csv);}
  else csv.append(RelatorioUsoController.campo(caminho)).append(',').append(RelatorioUsoController.campo(node.isNull()?null:node.asText())).append("\r\n");
 }
}
