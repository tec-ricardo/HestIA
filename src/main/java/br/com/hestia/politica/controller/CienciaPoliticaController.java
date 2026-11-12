package br.com.hestia.politica.controller;
import br.com.hestia.usuario.model.Usuario;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.util.*;
@RestController @RequestMapping("/ciencias-politica")
public class CienciaPoliticaController {
 private final JdbcTemplate jdbc;
 public CienciaPoliticaController(JdbcTemplate jdbc){this.jdbc=jdbc;}
 @GetMapping public Object listar(@RequestAttribute("usuarioAutenticado") Usuario u){
  return jdbc.queryForList("SELECT v.id,v.titulo,v.conteudo,v.versao,v.revisao,c.registrada_em FROM politica_uso_versoes v JOIN politicas_uso p ON p.id=v.politica_uso_id AND p.revisao=v.revisao LEFT JOIN ciencias_politica c ON c.politica_versao_id=v.id AND c.usuario_id=? WHERE p.empresa_id=? AND p.ativa=true AND p.excluida=false ORDER BY p.id",u.getId(),u.getEmpresa().getId());
 }
 @GetMapping("/historico") public Object historico(@RequestAttribute("usuarioAutenticado") Usuario u){
  return jdbc.queryForList("SELECT v.titulo,v.versao,v.revisao,c.registrada_em FROM ciencias_politica c JOIN politica_uso_versoes v ON v.id=c.politica_versao_id WHERE c.empresa_id=? AND c.usuario_id=? ORDER BY c.registrada_em DESC",u.getEmpresa().getId(),u.getId());
 }
 @PostMapping("/{versaoId}") @Transactional public Object registrar(@RequestAttribute("usuarioAutenticado") Usuario u,@PathVariable Long versaoId){
  jdbc.queryForObject("SELECT id FROM usuarios WHERE id=? AND empresa_id=? FOR UPDATE",Long.class,u.getId(),u.getEmpresa().getId());
  var versoes=jdbc.queryForList("SELECT v.id FROM politica_uso_versoes v JOIN politicas_uso p ON p.id=v.politica_uso_id AND p.revisao=v.revisao WHERE v.id=? AND p.empresa_id=? AND p.ativa=true AND p.excluida=false",versaoId,u.getEmpresa().getId());
  if(versoes.isEmpty())throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Versão vigente indisponível; consulte novamente a política.");
  if(jdbc.queryForObject("SELECT COUNT(*) FROM ciencias_politica WHERE usuario_id=? AND politica_versao_id=?",Long.class,u.getId(),versaoId)==0)
   jdbc.update("INSERT INTO ciencias_politica(empresa_id,usuario_id,politica_versao_id,registrada_em) VALUES(?,?,?,?)",u.getEmpresa().getId(),u.getId(),versaoId,java.time.LocalDateTime.now());
  return Map.of("registrada",true);
 }
}
