package br.com.hestia.conformidade;
import br.com.hestia.conformidade.service.AvaliacaoAutomaticaService;
import br.com.hestia.registro.repository.RegistroUsoIARepository;
import br.com.hestia.politica.repository.*;
import br.com.hestia.conformidade.repository.AvaliacaoConformidadeRepository;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.context.ApplicationEventPublisher;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:auto_conformidade;MODE=PostgreSQL;DB_CLOSE_DELAY=-1","spring.flyway.enabled=true","spring.jpa.hibernate.ddl-auto=validate","hestia.conformidade.automatica=true","hestia.catalogo.demonstracao=true"})
class AvaliacaoAutomaticaIntegrationTest {
 @Autowired JdbcTemplate jdbc; @Autowired AvaliacaoAutomaticaService service; @Autowired RegistroUsoIARepository registros;
 @Autowired PlatformTransactionManager manager; @Autowired PoliticaUsoRepository politicas; @Autowired PoliticaUsoVersaoRepository versoes;
 @Autowired AvaliacaoConformidadeRepository avaliacoes; @Autowired ApplicationEventPublisher eventos;
 @Test void confirmaDepoisDoCommitRecuperaFalhaEPreservaResultado(){
  jdbc.update("INSERT INTO empresas(id,nome,cnpj) VALUES(9001,'Teste','99999999000001')");
  jdbc.update("INSERT INTO departamentos(id,nome,empresa_id) VALUES(9001,'Teste',9001)");
  jdbc.update("INSERT INTO usuarios(id,nome,email,senha,cargo,perfil,ativo,data_cadastro,empresa_id,departamento_id) VALUES(9001,'Teste','auto@test.invalid','hash','Teste','ADMIN',true,CURRENT_TIMESTAMP,9001,9001)");
  jdbc.update("INSERT INTO ferramentas_ia(id,nome,fornecedor,tipo,finalidade_uso,status,nivel_risco,trata_dados_pessoais,data_cadastro,data_atualizacao,empresa_id) VALUES(9001,'Teste','Teste','CHATBOT','Teste','APROVADA','BAIXO',false,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,9001)");
  for(long id=9001;id<=9003;id++)jdbc.update("INSERT INTO registros_uso_ia(id,usuario_id,ferramenta_id,modelo_ia,data_hora,finalidade,status_execucao) VALUES(?,9001,9001,'Teste',CURRENT_TIMESTAMP,'PESQUISA','SUCESSO')",id);
  var tx=new TransactionTemplate(manager);
  tx.executeWithoutResult(s->{service.agendar(registros.findById(9001L).orElseThrow());assertEquals("PENDENTE",jdbc.queryForObject("SELECT estado FROM processamento_avaliacao WHERE registro_id=9001",String.class));});
  assertEquals("CONCLUIDA",jdbc.queryForObject("SELECT estado FROM processamento_avaliacao WHERE registro_id=9001",String.class));
  assertEquals("REQUER_REVISAO",jdbc.queryForObject("SELECT resultado FROM processamento_avaliacao WHERE registro_id=9001",String.class));
  service.processar(9001L);assertEquals(1,jdbc.queryForObject("SELECT tentativas FROM processamento_avaliacao WHERE registro_id=9001",Integer.class));
  tx.executeWithoutResult(s->{service.agendar(registros.findById(9002L).orElseThrow());s.setRollbackOnly();});
  assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM processamento_avaliacao WHERE registro_id=9002",Integer.class));
  jdbc.update("INSERT INTO processamento_avaliacao(registro_id,empresa_id,estado,snapshot_ferramenta,capturado_em,atualizado_em) VALUES(9003,9001,'PENDENTE','APROVADA',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)");
  var indisponivel=mock(RegistroUsoIARepository.class);when(indisponivel.findById(9003L)).thenThrow(new IllegalStateException("indisponível"));
  var falha=new AvaliacaoAutomaticaService(jdbc,indisponivel,politicas,versoes,avaliacoes,eventos,manager,true);falha.processar(9003L);
  assertEquals("FALHA_TECNICA",jdbc.queryForObject("SELECT estado FROM processamento_avaliacao WHERE registro_id=9003",String.class));
  service.processar(9003L);assertEquals("CONCLUIDA",jdbc.queryForObject("SELECT estado FROM processamento_avaliacao WHERE registro_id=9003",String.class));
  assertEquals(2,jdbc.queryForObject("SELECT COUNT(*) FROM tentativas_avaliacao WHERE registro_id=9003",Integer.class));
 }

 @Test void concorrenciaPreservaPoliticaETratamentoUnico() throws Exception {
  jdbc.update("INSERT INTO empresas(id,nome,cnpj) VALUES(9010,'Concorrência','99999999000010')");
  jdbc.update("INSERT INTO departamentos(id,nome,empresa_id) VALUES(9010,'Teste',9010)");
  jdbc.update("INSERT INTO usuarios(id,nome,email,senha,cargo,perfil,ativo,data_cadastro,empresa_id,departamento_id) VALUES(9010,'Teste','concurrent@test.invalid','hash','Teste','ADMIN',true,CURRENT_TIMESTAMP,9010,9010)");
  jdbc.update("INSERT INTO ferramentas_ia(id,nome,fornecedor,tipo,finalidade_uso,status,nivel_risco,trata_dados_pessoais,data_cadastro,data_atualizacao,empresa_id) VALUES(9010,'Teste','Teste','CHATBOT','Teste','BLOQUEADA','ALTO',false,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,9010)");
  var instante=java.time.LocalDateTime.now();
  jdbc.update("INSERT INTO politicas_uso(id,titulo,descricao,conteudo,versao,ativa,data_criacao,data_atualizacao,empresa_id) VALUES(9010,'Original','Descrição','Texto original','1',true,?,?,9010)",instante.minusDays(1),instante.minusDays(1));
  jdbc.update("INSERT INTO politica_uso_versoes(id,politica_uso_id,empresa_id,revisao,titulo,descricao,conteudo,versao,ativa,excluida,origem,registrada_em) VALUES(9010,9010,9010,1,'Original','Descrição','Texto original','1',true,false,'CRIACAO',?)",instante.minusDays(1));
  jdbc.update("INSERT INTO registros_uso_ia(id,usuario_id,ferramenta_id,modelo_ia,data_hora,finalidade,status_execucao) VALUES(9010,9010,9010,'Teste',CURRENT_TIMESTAMP,'PESQUISA','SUCESSO')");
  var semEntrega=new AvaliacaoAutomaticaService(jdbc,registros,politicas,versoes,avaliacoes,mock(ApplicationEventPublisher.class),manager,true);
  new TransactionTemplate(manager).executeWithoutResult(s->semEntrega.agendar(registros.findById(9010L).orElseThrow()));
  jdbc.update("UPDATE politicas_uso SET revisao=2,conteudo='Novo texto' WHERE id=9010");
  jdbc.update("INSERT INTO politica_uso_versoes(id,politica_uso_id,empresa_id,revisao,titulo,descricao,conteudo,versao,ativa,excluida,origem,registrada_em) VALUES(9011,9010,9010,2,'Nova','Descrição','Novo texto','2',true,false,'ALTERACAO',?)",instante.plusSeconds(1));
  try(var executor=java.util.concurrent.Executors.newFixedThreadPool(2)){
   var inicio=new java.util.concurrent.CountDownLatch(1);
   java.util.concurrent.Callable<Void> tarefa=()->{inicio.await();service.processar(9010L);return null;};
   var primeira=executor.submit(tarefa);var segunda=executor.submit(tarefa);inicio.countDown();
   primeira.get(30,java.util.concurrent.TimeUnit.SECONDS);segunda.get(30,java.util.concurrent.TimeUnit.SECONDS);
  }
  assertEquals("CONCLUIDA",jdbc.queryForObject("SELECT estado FROM processamento_avaliacao WHERE registro_id=9010",String.class));
  assertEquals("NAO_CONFORME",jdbc.queryForObject("SELECT resultado FROM processamento_avaliacao WHERE registro_id=9010",String.class));
  assertEquals(9010L,jdbc.queryForObject("SELECT politica_versao_id FROM avaliacoes_conformidade WHERE registro_uso_id=9010",Long.class));
  assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM avaliacoes_conformidade WHERE registro_uso_id=9010",Integer.class));
  assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM tratamentos_nao_conformidade WHERE empresa_id=9010",Integer.class));
  assertEquals(1,jdbc.queryForObject("SELECT tentativas FROM processamento_avaliacao WHERE registro_id=9010",Integer.class));
 }

 @Autowired br.com.hestia.usuario.repository.UsuarioRepository usuarios;
 @Autowired br.com.hestia.autenticacao.repository.RecuperacaoSenhaRepository recuperacoes;
 @Autowired br.com.hestia.autenticacao.repository.SessaoUsuarioRepository sessoes;
 @Autowired org.springframework.security.crypto.password.PasswordEncoder encoder;
 @Test void duasRedefinicoesConcorrentesConsomemTokenUmaVez() throws Exception {
  jdbc.update("INSERT INTO empresas(id,nome,cnpj) VALUES(9020,'Recuperação','99999999000020')");
  jdbc.update("INSERT INTO departamentos(id,nome,empresa_id) VALUES(9020,'Teste',9020)");
  jdbc.update("INSERT INTO usuarios(id,nome,email,senha,cargo,perfil,ativo,data_cadastro,empresa_id,departamento_id) VALUES(9020,'Teste','senha@test.invalid','hash','Teste','ADMIN',true,CURRENT_TIMESTAMP,9020,9020)");
  var entrega=mock(br.com.hestia.autenticacao.service.EntregaRecuperacaoSenha.class);
  var servico=new br.com.hestia.autenticacao.service.RecuperacaoSenhaService(usuarios,recuperacoes,encoder,entrega);
  org.springframework.test.util.ReflectionTestUtils.setField(servico,"sessoes",sessoes);
  var tx=new TransactionTemplate(manager);String token=tx.execute(st->servico.solicitar("senha@test.invalid"));
  try(var executor=java.util.concurrent.Executors.newFixedThreadPool(2)){
   var inicio=new java.util.concurrent.CountDownLatch(1);
   java.util.concurrent.Callable<Boolean> tentativa=()->{inicio.await();try{tx.executeWithoutResult(st->servico.redefinir(token,"SenhaNova123!"));return true;}catch(IllegalArgumentException e){return false;}};
   var a=executor.submit(tentativa);var b=executor.submit(tentativa);inicio.countDown();
   assertNotEquals(a.get(30,java.util.concurrent.TimeUnit.SECONDS),b.get(30,java.util.concurrent.TimeUnit.SECONDS));
  }
  assertTrue(encoder.matches("SenhaNova123!",jdbc.queryForObject("SELECT senha FROM usuarios WHERE id=9020",String.class)));
  assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM recuperacoes_senha WHERE usuario_id=9020 AND utilizada_em IS NOT NULL",Integer.class));
 }

 @Test void evidenciasCompletasGeramUmaUnicaRecompensa() {
  jdbc.update("INSERT INTO empresas(id,nome,cnpj) VALUES(9030,'Evidências','99999999000030')");
  jdbc.update("INSERT INTO departamentos(id,nome,empresa_id) VALUES(9030,'Teste',9030)");
  jdbc.update("INSERT INTO usuarios(id,nome,email,senha,cargo,perfil,ativo,data_cadastro,empresa_id,departamento_id) VALUES(9030,'Teste','evidencias@test.invalid','hash','Teste','ADMIN',true,CURRENT_TIMESTAMP,9030,9030)");
  jdbc.update("INSERT INTO ferramentas_ia(id,nome,fornecedor,tipo,finalidade_uso,status,nivel_risco,trata_dados_pessoais,data_cadastro,data_atualizacao,empresa_id) VALUES(9030,'Teste','Teste','CHATBOT','Teste','APROVADA','BAIXO',false,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,9030)");
  jdbc.update("INSERT INTO politicas_uso(id,titulo,descricao,conteudo,versao,ativa,data_criacao,data_atualizacao,empresa_id) VALUES(9030,'Política','Descrição','Regras de ensaio','1',true,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,9030)");
  jdbc.update("INSERT INTO politica_uso_versoes(id,politica_uso_id,empresa_id,revisao,titulo,descricao,conteudo,versao,ativa,excluida,origem,registrada_em) VALUES(9030,9030,9030,1,'Política','Descrição','Regras de ensaio','1',true,false,'CRIACAO',?)",java.time.LocalDateTime.now().minusDays(1));
  jdbc.update("INSERT INTO registros_uso_ia(id,usuario_id,ferramenta_id,modelo_ia,data_hora,finalidade,status_execucao) VALUES(9030,9030,9030,'Teste',CURRENT_TIMESTAMP,'PESQUISA','SUCESSO')");
  var semEntrega=new AvaliacaoAutomaticaService(jdbc,registros,politicas,versoes,avaliacoes,mock(ApplicationEventPublisher.class),manager,true);
  new TransactionTemplate(manager).executeWithoutResult(st->{
   semEntrega.agendar(registros.findById(9030L).orElseThrow());
   for(var criterio:java.util.List.of("FINALIDADE_ADEQUADA","DADOS_SENSIVEIS","VALIDACAO_HUMANA","LGPD"))jdbc.update("INSERT INTO evidencias_criterios_uso VALUES(?,?,'ATENDIDO',?,9030,CURRENT_TIMESTAMP)",9030,criterio,"Regra da política e referência de ensaio: "+criterio);
  });
  service.processar(9030L);service.processar(9030L);
  assertEquals("CONCLUIDA",jdbc.queryForObject("SELECT estado FROM processamento_avaliacao WHERE registro_id=9030",String.class));
  assertEquals("CONFORME",jdbc.queryForObject("SELECT resultado FROM processamento_avaliacao WHERE registro_id=9030",String.class));
  assertEquals(7,jdbc.queryForObject("SELECT COUNT(*) FROM fundamentos_criterios_avaliacao WHERE registro_id=9030 AND estado='ATENDIDO'",Integer.class));
  assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM movimentos_experiencia WHERE usuario_id=9030 AND referencia LIKE 'AVALIACAO_CONFORME:%'",Integer.class));
  jdbc.update("INSERT INTO registros_uso_ia(id,usuario_id,ferramenta_id,modelo_ia,data_hora,finalidade,status_execucao) VALUES(9031,9030,9030,'Teste',?,'PESQUISA','SUCESSO')",java.time.LocalDateTime.now().minusMinutes(1));
  new TransactionTemplate(manager).executeWithoutResult(st->{semEntrega.agendar(registros.findById(9031L).orElseThrow());jdbc.update("INSERT INTO evidencias_criterios_uso VALUES(9031,'LGPD','NAO_ATENDIDO','Referência de violação confirmada em ensaio',9030,CURRENT_TIMESTAMP)");});
  service.processar(9031L);
  assertEquals("NAO_CONFORME",jdbc.queryForObject("SELECT resultado FROM processamento_avaliacao WHERE registro_id=9031",String.class));
  assertEquals("NAO_VERIFICADO",jdbc.queryForObject("SELECT estado FROM fundamentos_criterios_avaliacao WHERE registro_id=9031 AND criterio='IA_AUTORIZADA'",String.class));
  assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM trilha_pendencias_catalogo WHERE empresa_id=9030 AND tema='USO_DE_FERRAMENTAS_AUTORIZADAS'",Integer.class));

 }
}
