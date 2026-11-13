package br.com.hestia.registro.controller;
import br.com.hestia.usuario.model.*;
import br.com.hestia.registro.model.FinalidadeUso;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDate;
import java.util.*;
@RestController @RequestMapping("/relatorios-uso")
public class RelatorioUsoController {
 private final JdbcTemplate jdbc;
 public RelatorioUsoController(JdbcTemplate jdbc){this.jdbc=jdbc;}
 @GetMapping public Object consultar(@RequestAttribute("usuarioAutenticado")Usuario u,@RequestParam(required=false)LocalDate inicio,@RequestParam(required=false)LocalDate fim,@RequestParam(required=false)Long departamentoId,@RequestParam(required=false)Long ferramentaId,@RequestParam(required=false)FinalidadeUso finalidade){return linhas(u,inicio,fim,departamentoId,ferramentaId,finalidade);}
 @GetMapping(value="/exportar",produces="text/csv") public ResponseEntity<String> exportar(@RequestAttribute("usuarioAutenticado")Usuario u,@RequestParam(required=false)LocalDate inicio,@RequestParam(required=false)LocalDate fim,@RequestParam(required=false)Long departamentoId,@RequestParam(required=false)Long ferramentaId,@RequestParam(required=false)FinalidadeUso finalidade){
  var rows=linhas(u,inicio,fim,departamentoId,ferramentaId,finalidade);StringBuilder csv=new StringBuilder("id,data,usuario,departamento,ferramenta,finalidade,tokens,custo_estimado,status\r\n");
  for(var r:rows){csv.append(String.join(",",List.of(campo(r.get("id")),campo(r.get("data")),campo(r.get("usuario")),campo(r.get("departamento")),campo(r.get("ferramenta")),campo(r.get("finalidade")),campo(r.get("tokens")),campo(r.get("custo_estimado")),campo(r.get("status"))))).append("\r\n");}
  return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=relatorio-usos.csv").contentType(MediaType.parseMediaType("text/csv;charset=UTF-8")).body(csv.toString());
 }
 public List<Map<String,Object>> linhas(Usuario u,LocalDate inicio,LocalDate fim,Long departamento,Long ferramenta,FinalidadeUso finalidade){
  if(u.getPerfil()==PerfilUsuario.FUNCIONARIO)throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Relatório gerencial restrito a gestor e administrador");
  if(inicio!=null&&fim!=null&&inicio.isAfter(fim))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Período inválido");
  var args=new ArrayList<Object>();args.add(u.getEmpresa().getId());String filtro="";
  if(u.getPerfil()==PerfilUsuario.GESTOR){if(u.getDepartamento()==null||departamento!=null&&!departamento.equals(u.getDepartamento().getId()))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Departamento não autorizado");departamento=u.getDepartamento().getId();}
  if(departamento!=null){filtro+=" AND u.departamento_id=?";args.add(departamento);}
  if(ferramenta!=null){filtro+=" AND r.ferramenta_id=?";args.add(ferramenta);}
  if(finalidade!=null){filtro+=" AND r.finalidade=?";args.add(finalidade.name());}
  if(inicio!=null){filtro+=" AND r.data_hora>=?";args.add(inicio.atStartOfDay());}
  if(fim!=null){filtro+=" AND r.data_hora<?";args.add(fim.plusDays(1).atStartOfDay());}
  return jdbc.queryForList("SELECT r.id,r.data_hora AS data,u.nome AS usuario,d.nome AS departamento,f.nome AS ferramenta,r.finalidade,r.tokens_total AS tokens,r.custo_estimado,r.status_execucao AS status FROM registros_uso_ia r JOIN usuarios u ON u.id=r.usuario_id JOIN departamentos d ON d.id=u.departamento_id JOIN ferramentas_ia f ON f.id=r.ferramenta_id WHERE u.empresa_id=? AND d.empresa_id=u.empresa_id AND f.empresa_id=u.empresa_id"+filtro+" ORDER BY r.data_hora,r.id",args.toArray());
 }
 public static String campo(Object valor){if(valor==null)return "";String s=String.valueOf(valor);if(valor instanceof String&&s.stripLeading().matches("^[=+@\\-].*"))s="'"+s;return "\""+s.replace("\"","\"\"")+"\"";}
}
