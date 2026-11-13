package br.com.hestia.conformidade.controller;
import br.com.hestia.usuario.model.*;
import br.com.hestia.conformidade.service.AvaliacaoAutomaticaService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;
@RestController @RequestMapping("/avaliacoes-automaticas")
public class AvaliacaoAutomaticaController {
 private final JdbcTemplate jdbc;private final AvaliacaoAutomaticaService service;
 public AvaliacaoAutomaticaController(JdbcTemplate jdbc,AvaliacaoAutomaticaService service){this.jdbc=jdbc;this.service=service;}
 @GetMapping public Object listar(@RequestAttribute("usuarioAutenticado")Usuario u){
  var args=new ArrayList<Object>();args.add(u.getEmpresa().getId());String filtro="";
  if(u.getPerfil()==PerfilUsuario.FUNCIONARIO){filtro=" AND r.usuario_id=?";args.add(u.getId());}
  if(u.getPerfil()==PerfilUsuario.GESTOR){filtro=" AND v.departamento_id=?";args.add(u.getDepartamento().getId());}
  return jdbc.queryForList("SELECT p.*,r.data_hora,v.nome AS usuario_nome FROM processamento_avaliacao p JOIN registros_uso_ia r ON r.id=p.registro_id JOIN usuarios v ON v.id=r.usuario_id WHERE p.empresa_id=?"+filtro+" ORDER BY p.registro_id DESC",args.toArray());
 }
 @GetMapping("/{id}/fundamentos") public Object fundamentos(@RequestAttribute("usuarioAutenticado")Usuario u,@PathVariable Long id){
  @SuppressWarnings("unchecked") var disponiveis=(List<Map<String,Object>>)listar(u);
  if(disponiveis.stream().noneMatch(r->((Number)r.get("registro_id")).longValue()==id))throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Processamento indisponível");
  return Map.of("criterios",jdbc.queryForList("SELECT criterio,estado,fundamento FROM fundamentos_criterios_avaliacao WHERE registro_id=? ORDER BY criterio",id),"tentativas",jdbc.queryForList("SELECT tentativa,estado,registrada_em FROM tentativas_avaliacao WHERE registro_id=? ORDER BY tentativa",id));
 }
 @PostMapping("/{id}/tentar-novamente") public Object repetir(@RequestAttribute("usuarioAutenticado")Usuario u,@PathVariable Long id){
  if(u.getPerfil()==PerfilUsuario.FUNCIONARIO)throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Solicite a nova tentativa ao gestor");
  var rows=jdbc.queryForList("SELECT p.estado,v.departamento_id FROM processamento_avaliacao p JOIN registros_uso_ia r ON r.id=p.registro_id JOIN usuarios v ON v.id=r.usuario_id WHERE p.registro_id=? AND p.empresa_id=?",id,u.getEmpresa().getId());
  if(rows.isEmpty()||u.getPerfil()==PerfilUsuario.GESTOR&&!u.getDepartamento().getId().equals(((Number)rows.getFirst().get("departamento_id")).longValue()))throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Processamento indisponível");
  service.processar(id);return Map.of("consultarResultado",true);
 }
}
