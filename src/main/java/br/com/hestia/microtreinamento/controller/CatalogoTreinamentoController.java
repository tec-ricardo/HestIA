package br.com.hestia.microtreinamento.controller;
import br.com.hestia.aprendizagem.*;
import br.com.hestia.usuario.model.Usuario;
import br.com.hestia.microtreinamento.model.TemaTreinamento;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import java.util.*;
import java.time.LocalDateTime;
import static br.com.hestia.aprendizagem.AcessoCapacitacao.*;
@RestController @RequestMapping("/catalogo-treinamentos")
public class CatalogoTreinamentoController {
 private final AvaliacaoAprendizagemService a;
 public CatalogoTreinamentoController(AvaliacaoAprendizagemService a){this.a=a;}
 public record Conteudo(Long anteriorId,String titulo,String descricao,String conteudo,TemaTreinamento tema,Integer duracaoMinutos,String fonte,String autorizacaoUso,String objetivo,String publico,String atividade,String criterioAceite,List<ContratoAvaliacao.Questao> questoes){}
 public record Transicao(String estado,String motivo){}
 @GetMapping public Object listar(@RequestAttribute("usuarioAutenticado")Usuario u){a.acesso().administrador(u);return a.jdbc().queryForList("SELECT id,anterior_id,titulo,descricao,tema,fonte,estado,criado_em,revisado_em,publicado_em,retirado_em FROM catalogo_treinamentos WHERE empresa_id=? ORDER BY id DESC",a.acesso().empresa(u));}
 @PostMapping @Transactional public Object criar(@RequestAttribute("usuarioAutenticado")Usuario u,@RequestBody Conteudo c){
  a.acesso().administrador(u);exigir(c!=null&&c.tema()!=null&&c.duracaoMinutos()!=null&&c.duracaoMinutos()>0,HttpStatus.BAD_REQUEST,"Tema e duração obrigatórios");
  exigir(texto(c.titulo(),250)&&texto(c.descricao(),2000)&&texto(c.conteudo(),200000)&&texto(c.fonte(),2000)&&texto(c.autorizacaoUso(),2000)&&texto(c.objetivo(),2000)&&texto(c.publico(),2000)&&texto(c.atividade(),10000)&&texto(c.criterioAceite(),10000),HttpStatus.BAD_REQUEST,"Preencha fonte, autorização, objetivo, público, conteúdo e atividade com critério de aceite");
  AvaliacaoAprendizagemService.validarQuestoes(c.questoes());
  if(c.anteriorId()!=null)obter(u,c.anteriorId());
  long id=a.inserir("INSERT INTO catalogo_treinamentos(empresa_id,anterior_id,titulo,descricao,conteudo,tema,duracao_minutos,fonte,autorizacao_uso,objetivo,publico,atividade,criterio_aceite,questionario,conteudo_sha256,estado,criado_por,criado_em) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,'RASCUNHO',?,?)",a.acesso().empresa(u),c.anteriorId(),c.titulo(),c.descricao(),c.conteudo(),c.tema().name(),c.duracaoMinutos(),c.fonte(),c.autorizacaoUso(),c.objetivo(),c.publico(),c.atividade(),c.criterioAceite(),a.escrever(c.questoes()),hash(c.conteudo()),u.getId(),LocalDateTime.now());
  return Map.of("id",id,"estado","RASCUNHO");
 }
 @GetMapping("/{id}") public Object consultar(@RequestAttribute("usuarioAutenticado")Usuario u,@PathVariable Long id){a.acesso().administrador(u);return obter(u,id);}
 @PostMapping("/{id}/estado") @Transactional public Object transicionar(@RequestAttribute("usuarioAutenticado")Usuario u,@PathVariable Long id,@RequestBody Transicao t){
  a.acesso().administrador(u);a.acesso().serializar(u.getId());
  var rows=a.jdbc().queryForList("SELECT * FROM catalogo_treinamentos WHERE id=? AND empresa_id=? FOR UPDATE",id,a.acesso().empresa(u));exigir(!rows.isEmpty(),HttpStatus.NOT_FOUND,"Conteúdo indisponível");var c=rows.getFirst();String atual=String.valueOf(c.get("estado"));
  exigir(t!=null&&t.estado()!=null,HttpStatus.BAD_REQUEST,"Estado obrigatório");
  if(atual.equals(t.estado()))return Map.of("estado",atual);
  boolean permitido=atual.equals("RASCUNHO")&&t.estado().equals("EM_REVISAO")||atual.equals("EM_REVISAO")&&t.estado().equals("PUBLICADO")||atual.equals("PUBLICADO")&&t.estado().equals("RETIRADO");
  exigir(permitido,HttpStatus.CONFLICT,"Transição inválida; revisões de conteúdo exigem uma nova versão");
  if(t.estado().equals("PUBLICADO")){
   exigir(texto(t.motivo(),2000),HttpStatus.BAD_REQUEST,"Registre a verificação de fonte, acesso, autorização e adequação");
   a.inserir("INSERT INTO aprendizagem_catalogo(empresa_id,treinamento_id,versao,conteudo_sha256,tema,questionario,publicado_por,publicado_em) VALUES(?,?,?,?,?,?,?,?)",a.acesso().empresa(u),id,1,c.get("conteudo_sha256"),c.get("tema"),c.get("questionario"),u.getId(),LocalDateTime.now());
   a.jdbc().update("UPDATE catalogo_treinamentos SET estado='PUBLICADO',revisado_por=?,revisado_em=?,publicado_em=?,evidencia_revisao=? WHERE id=?",u.getId(),LocalDateTime.now(),LocalDateTime.now(),t.motivo(),id);
  }else if(t.estado().equals("RETIRADO")){
   exigir(texto(t.motivo(),2000),HttpStatus.BAD_REQUEST,"Motivo de retirada obrigatório");
   a.jdbc().update("UPDATE catalogo_treinamentos SET estado='RETIRADO',retirado_em=?,motivo_retirada=? WHERE id=?",LocalDateTime.now(),t.motivo(),id);
  }else a.jdbc().update("UPDATE catalogo_treinamentos SET estado='EM_REVISAO' WHERE id=?",id);
  return Map.of("estado",t.estado());
 }
 private Map<String,Object> obter(Usuario u,Long id){return a.jdbc().queryForList("SELECT * FROM catalogo_treinamentos WHERE id=? AND empresa_id=?",id,a.acesso().empresa(u)).stream().findFirst().orElseThrow(()->erro(HttpStatus.NOT_FOUND,"Conteúdo indisponível"));}
 private boolean texto(String s,int limite){return s!=null&&!s.isBlank()&&s.length()<=limite;}
}
