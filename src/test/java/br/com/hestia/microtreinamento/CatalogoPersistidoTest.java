package br.com.hestia.microtreinamento;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import br.com.hestia.aprendizagem.*;
import br.com.hestia.microtreinamento.controller.CatalogoTreinamentoController;
import br.com.hestia.microtreinamento.service.CatalogoTreinamentoService;
import br.com.hestia.microtreinamento.model.TemaTreinamento;
import br.com.hestia.autenticacao.contexto.*;
import br.com.hestia.usuario.model.Usuario;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class CatalogoPersistidoTest {
 @Test void publicaRetiraEPreservaAvaliacaoSemMisturarEmpresas() throws Exception {
  var jdbc=new JdbcTemplate(new DriverManagerDataSource("jdbc:h2:mem:catalogo_"+System.nanoTime()+";MODE=PostgreSQL;DB_CLOSE_DELAY=-1","sa",""));
  jdbc.execute("CREATE TABLE empresas(id BIGINT PRIMARY KEY)");jdbc.execute("CREATE TABLE usuarios(id BIGINT PRIMARY KEY)");jdbc.execute("INSERT INTO empresas VALUES(10),(20)");jdbc.execute("INSERT INTO usuarios VALUES(1)");
  var script=new String(getClass().getResourceAsStream("/db/migration/V26__catalogo_treinamentos.sql").readAllBytes(),java.nio.charset.StandardCharsets.UTF_8);jdbc.execute(script);
  jdbc.execute("CREATE TABLE aprendizagem_catalogo(id BIGSERIAL PRIMARY KEY,empresa_id BIGINT,treinamento_id BIGINT,versao INT,conteudo_sha256 VARCHAR,tema VARCHAR,questionario VARCHAR,publicado_por BIGINT,publicado_em TIMESTAMP, UNIQUE(empresa_id,treinamento_id,versao))");
  var acesso=mock(AcessoCapacitacao.class);var u=mock(Usuario.class);when(u.getId()).thenReturn(1L);when(acesso.empresa(u)).thenReturn(10L);
  var a=new AvaliacaoAprendizagemService(jdbc,acesso,mock(org.springframework.context.ApplicationEventPublisher.class));var c=new CatalogoTreinamentoController(a);
  var questoes=java.util.stream.IntStream.range(1,6).mapToObj(i->new ContratoAvaliacao.Questao("q"+i,"Enunciado",Map.of("A","Sim","B","Não","C","Outro"),"A","Explicação")).toList();
  var dados=new CatalogoTreinamentoController.Conteudo(null,"Curso","Descrição","Conteúdo autorizado",TemaTreinamento.LGPD,10,"Fonte","Autorização","Objetivo","Público","Atividade","Aceite",questoes);
  var criado=(Map<?,?>)c.criar(u,dados);long id=((Number)criado.get("id")).longValue();
  var empresa=new EmpresaAtual();empresa.definir(new ContextoEmpresa(1L,10L));var catalogo=new CatalogoTreinamentoService(jdbc,empresa,false);
  assertTrue(catalogo.criarTreinamentos().isEmpty());
  c.transicionar(u,id,new CatalogoTreinamentoController.Transicao("EM_REVISAO",""));
  c.transicionar(u,id,new CatalogoTreinamentoController.Transicao("PUBLICADO","Fonte e acesso conferidos"));
  c.transicionar(u,id,new CatalogoTreinamentoController.Transicao("PUBLICADO","Reenvio"));
  assertEquals(1,catalogo.criarTreinamentos().size());assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM aprendizagem_catalogo",Integer.class));
  empresa.definir(new ContextoEmpresa(1L,20L));assertTrue(catalogo.criarTreinamentos().isEmpty());empresa.definir(new ContextoEmpresa(1L,10L));
  c.transicionar(u,id,new CatalogoTreinamentoController.Transicao("RETIRADO","Atualização necessária"));assertTrue(catalogo.criarTreinamentos().isEmpty());
  assertTrue(catalogo.historico(id).isPresent());
  empresa.definir(new ContextoEmpresa(1L,20L));assertTrue(catalogo.historico(id).isEmpty());
  assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM aprendizagem_catalogo",Integer.class));
 }
}
