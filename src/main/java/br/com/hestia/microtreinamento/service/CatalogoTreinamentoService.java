package br.com.hestia.microtreinamento.service;
import org.springframework.stereotype.Service;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Value;
import br.com.hestia.autenticacao.contexto.EmpresaAtual;
import br.com.hestia.microtreinamento.model.*;
import java.util.List;
@Service
public class CatalogoTreinamentoService extends MicrotreinamentoFicticioService {
 private final JdbcTemplate jdbc; private final EmpresaAtual empresa; private final boolean demonstracao;
 public CatalogoTreinamentoService(JdbcTemplate jdbc,EmpresaAtual empresa,@Value("${hestia.catalogo.demonstracao:false}") boolean demonstracao){this.jdbc=jdbc;this.empresa=empresa;this.demonstracao=demonstracao;}
 public boolean demonstracao(){return demonstracao;}
 @Override public List<Microtreinamento> criarTreinamentos(){
  if(demonstracao)return super.criarTreinamentos();
  return criarTreinamentos(empresa.obterId());
 }
 public List<Microtreinamento> criarTreinamentos(Long empresaId){
  java.util.Objects.requireNonNull(empresaId, "Empresa obrigatória");
  if(demonstracao)return super.criarTreinamentos();
  return jdbc.query("SELECT id,titulo,descricao,conteudo,tema,duracao_minutos FROM catalogo_treinamentos WHERE empresa_id=? AND estado='PUBLICADO' ORDER BY id",(rs,n)->new Microtreinamento(rs.getLong("id"),rs.getString("titulo"),rs.getString("descricao"),rs.getString("conteudo"),TemaTreinamento.valueOf(rs.getString("tema")),rs.getInt("duracao_minutos"),true),empresaId);
 }
 // Somente consultas históricas autorizadas usam versões retiradas; não reabrem novos cursos.
 public java.util.Optional<Microtreinamento> historico(Long id){
  if(demonstracao)return historico(null,id);
  return historico(empresa.obterId(),id);
 }
 java.util.Optional<Microtreinamento> historico(Long empresaId,Long id){
  if(demonstracao)return super.criarTreinamentos().stream().filter(t->t.getId().equals(id)).findFirst();
  return jdbc.query("SELECT id,titulo,descricao,conteudo,tema,duracao_minutos,estado FROM catalogo_treinamentos WHERE empresa_id=? AND id=? AND estado IN ('PUBLICADO','RETIRADO')",(rs,n)->new Microtreinamento(rs.getLong("id"),rs.getString("titulo"),rs.getString("descricao"),rs.getString("conteudo"),TemaTreinamento.valueOf(rs.getString("tema")),rs.getInt("duracao_minutos"),"PUBLICADO".equals(rs.getString("estado"))),java.util.Objects.requireNonNull(empresaId, "Empresa obrigatória"),id).stream().findFirst();
 }
}
