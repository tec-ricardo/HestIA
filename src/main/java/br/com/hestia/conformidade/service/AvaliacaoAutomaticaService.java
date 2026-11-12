package br.com.hestia.conformidade.service;
import br.com.hestia.registro.model.RegistroUsoIA;
import br.com.hestia.registro.repository.RegistroUsoIARepository;
import br.com.hestia.politica.repository.*;
import br.com.hestia.conformidade.repository.AvaliacaoConformidadeRepository;
import br.com.hestia.conformidade.model.*;
import br.com.hestia.conformidade.dto.FundamentosConformidade;
import org.springframework.stereotype.Service;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.event.*;
import java.time.LocalDateTime;
import java.util.*;
@Service
public class AvaliacaoAutomaticaService {
 private final JdbcTemplate jdbc; private final RegistroUsoIARepository registros;
 private final PoliticaUsoRepository politicas; private final PoliticaUsoVersaoRepository versoes;
 private final AvaliacaoConformidadeRepository avaliacoes; private final ApplicationEventPublisher eventos;
 private final TransactionTemplate transacao; private final boolean habilitada;
 public record UsoPersistido(Long registroId){}
 public AvaliacaoAutomaticaService(JdbcTemplate jdbc,RegistroUsoIARepository registros,PoliticaUsoRepository politicas,
   PoliticaUsoVersaoRepository versoes,AvaliacaoConformidadeRepository avaliacoes,ApplicationEventPublisher eventos,
   PlatformTransactionManager manager,@Value("${hestia.conformidade.automatica:true}") boolean habilitada){
  this.jdbc=jdbc;this.registros=registros;this.politicas=politicas;this.versoes=versoes;this.avaliacoes=avaliacoes;this.eventos=eventos;this.habilitada=habilitada;
  transacao=new TransactionTemplate(manager);transacao.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
 }
 public void agendar(RegistroUsoIA uso){
  if(!habilitada)return;
  // A última revisão conhecida em cada política no instante do uso fica congelada na fila.
  var candidatas=jdbc.queryForList("SELECT v.id FROM politica_uso_versoes v WHERE v.empresa_id=? AND v.registrada_em<=? AND v.ativa=true AND v.excluida=false AND NOT EXISTS (SELECT 1 FROM politica_uso_versoes posterior WHERE posterior.politica_uso_id=v.politica_uso_id AND posterior.registrada_em<=? AND posterior.revisao>v.revisao)",uso.getUsuario().getEmpresa().getId(),uso.getDataHora(),uso.getDataHora());
  Long politicaVersao=candidatas.size()==1?((Number)candidatas.getFirst().get("id")).longValue():null;
  jdbc.update("INSERT INTO processamento_avaliacao(registro_id,empresa_id,estado,snapshot_ferramenta,politica_versao_id,capturado_em,atualizado_em) VALUES(?,?,'PENDENTE',?,?,?,?)",uso.getId(),uso.getUsuario().getEmpresa().getId(),uso.getFerramenta().getStatus().name(),politicaVersao,LocalDateTime.now(),LocalDateTime.now());
  eventos.publishEvent(new UsoPersistido(uso.getId()));
 }
 @TransactionalEventListener(phase=TransactionPhase.AFTER_COMMIT)
 public void aposPersistencia(UsoPersistido e){processar(e.registroId());}
 public void processar(Long id){
  try{transacao.executeWithoutResult(s->executar(id));}
  catch(RuntimeException erro){
   transacao.executeWithoutResult(s->{
    var rows=jdbc.queryForList("SELECT estado,tentativas FROM processamento_avaliacao WHERE registro_id=? FOR UPDATE",id);
    if(rows.isEmpty()||"CONCLUIDA".equals(rows.getFirst().get("estado")))return;
    int tentativa=((Number)rows.getFirst().get("tentativas")).intValue()+1;
    jdbc.update("UPDATE processamento_avaliacao SET estado='FALHA_TECNICA',tentativas=?,motivo=?,atualizado_em=? WHERE registro_id=?",tentativa,"Falha técnica no processamento; solicite nova tentativa.",LocalDateTime.now(),id);
    jdbc.update("INSERT INTO tentativas_avaliacao(registro_id,tentativa,estado,registrada_em) VALUES(?,?,'FALHA_TECNICA',?)",id,tentativa,LocalDateTime.now());
   });
  }
 }
 private void executar(Long id){
  var rows=jdbc.queryForList("SELECT * FROM processamento_avaliacao WHERE registro_id=? FOR UPDATE",id);
  if(rows.isEmpty()||"CONCLUIDA".equals(rows.getFirst().get("estado")))return;
  var fila=rows.getFirst();var uso=registros.findById(id).orElseThrow();
  if(!uso.getUsuario().getEmpresa().getId().equals(((Number)fila.get("empresa_id")).longValue()))throw new IllegalStateException("Empresa incompatível");
  jdbc.update("UPDATE processamento_avaliacao SET estado='EM_PROCESSAMENTO' WHERE registro_id=?",id);
  int tentativa=((Number)fila.get("tentativas")).intValue()+1;
  var existentes=jdbc.queryForList("SELECT id,status FROM avaliacoes_conformidade WHERE registro_uso_id=?",id);
  var estados=new EnumMap<CriterioConformidade,DecisaoEvidencias.Estado>(CriterioConformidade.class);
  var fundamentos=new EnumMap<CriterioConformidade,String>(CriterioConformidade.class);
  for(var c:CriterioConformidade.values()){estados.put(c,DecisaoEvidencias.Estado.NAO_VERIFICADO);fundamentos.put(c,"Evidência não disponível; nenhum descumprimento foi inferido.");}
  estados.put(CriterioConformidade.REGISTRO_DE_USO,DecisaoEvidencias.Estado.ATENDIDO);
  fundamentos.put(CriterioConformidade.REGISTRO_DE_USO,"Uso persistido "+id+"; autor "+uso.getUsuario().getId()+"; finalidade "+uso.getFinalidade());
  Long avaliacaoId=null;String resultado="REQUER_REVISAO";
  String motivo="Evidências insuficientes: é necessário comprovar finalidade, dados sensíveis, validação humana e requisitos LGPD. Ausência de evidência não é descumprimento.";
  if(!existentes.isEmpty()){avaliacaoId=((Number)existentes.getFirst().get("id")).longValue();resultado=String.valueOf(existentes.getFirst().get("status"));motivo="Resultado já existente preservado, sem repetir efeitos.";}
  else {
   if(fila.get("politica_versao_id")==null)motivo="Política aplicável ausente ou ambígua no registro. Requer revisão; nenhuma política foi escolhida arbitrariamente.";
   else{
    var v=versoes.findById(((Number)fila.get("politica_versao_id")).longValue()).orElseThrow();var politica=politicas.findById(v.getPoliticaUsoId()).orElseThrow();
    if(!v.getEmpresaId().equals(uso.getUsuario().getEmpresa().getId()))throw new IllegalStateException("Política de outra empresa");
    estados.put(CriterioConformidade.POLITICA_INTERNA,DecisaoEvidencias.Estado.ATENDIDO);
    fundamentos.put(CriterioConformidade.POLITICA_INTERNA,"Revisão preservada "+v.getId()+"; SHA-256 "+br.com.hestia.aprendizagem.AcessoCapacitacao.hash(v.getConteudo()));
    var captura=((java.sql.Timestamp)fila.get("capturado_em")).toLocalDateTime();
    boolean contemporaneo=!uso.getDataHora().isBefore(captura.minusSeconds(5))&&!uso.getDataHora().isAfter(captura);
    String status=String.valueOf(fila.get("snapshot_ferramenta"));
    if(contemporaneo){
     if(status.equals("APROVADA"))estados.put(CriterioConformidade.IA_AUTORIZADA,DecisaoEvidencias.Estado.ATENDIDO);
     if(Set.of("BLOQUEADA","DESCONTINUADA").contains(status))estados.put(CriterioConformidade.IA_AUTORIZADA,DecisaoEvidencias.Estado.NAO_ATENDIDO);
     fundamentos.put(CriterioConformidade.IA_AUTORIZADA,"Status contemporâneo capturado no servidor: "+status);
    }
    for(var e:jdbc.queryForList("SELECT * FROM evidencias_criterios_uso WHERE registro_id=?",id)){
     var criterio=CriterioConformidade.valueOf(String.valueOf(e.get("criterio")));
     estados.put(criterio,DecisaoEvidencias.Estado.valueOf(String.valueOf(e.get("estado"))));
     fundamentos.put(criterio,"Conferência humana por "+e.get("conferido_por")+" em "+e.get("conferido_em")+": "+e.get("referencia"));
    }
    resultado=DecisaoEvidencias.decidir(estados).name();
    motivo="Resultado por critério: "+estados+". Consulte a matriz preservada de fundamentos e referências.";
    var atendidos=EnumSet.noneOf(CriterioConformidade.class);
    estados.forEach((c,estado)->{if(estado==DecisaoEvidencias.Estado.ATENDIDO)atendidos.add(c);});
    var a=new AvaliacaoConformidade(uso,politica,StatusConformidade.valueOf(resultado),atendidos,motivo);
    a.preservarFundamentos(v,"CONFORMIDADE_CRITERIOS","2.1",new FundamentosConformidade(motivo,"Critério não verificado não equivale a não atendido. Violação crítica comprovada prevalece; aprovação exige comprovação de todos os critérios.","Conferência humana minimizada, sem análise automática do conteúdo confidencial. Ausência de referência não comprova descumprimento. Dados retrospectivos não comprovam status histórico da ferramenta."));
    a=avaliacoes.saveAndFlush(a);avaliacaoId=a.getId();
    eventos.publishEvent(new br.com.hestia.trilha.service.MotorTrilhasService.ConformidadeRegistrada(uso.getUsuario().getId(),uso.getUsuario().getEmpresa().getId(),avaliacaoId));
   }
  }
  String evidencias=estados.toString();
  for(var c:CriterioConformidade.values())jdbc.update("INSERT INTO fundamentos_criterios_avaliacao(registro_id,criterio,estado,fundamento) VALUES(?,?,?,?)",id,c.name(),estados.get(c).name(),fundamentos.get(c));
  jdbc.update("UPDATE processamento_avaliacao SET estado='CONCLUIDA',resultado=?,avaliacao_id=?,motivo=?,evidencias=?,tentativas=?,atualizado_em=? WHERE registro_id=?",resultado,avaliacaoId,motivo,evidencias,tentativa,LocalDateTime.now(),id);
  jdbc.update("INSERT INTO tentativas_avaliacao(registro_id,tentativa,estado,registrada_em) VALUES(?,?,'CONCLUIDA',?)",id,tentativa,LocalDateTime.now());
 }
}
