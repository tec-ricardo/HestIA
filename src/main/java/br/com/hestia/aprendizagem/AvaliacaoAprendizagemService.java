package br.com.hestia.aprendizagem;

import br.com.hestia.usuario.model.Usuario;
import br.com.hestia.microtreinamento.service.MicrotreinamentoFicticioService;
import br.com.hestia.microtreinamento.model.Microtreinamento;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.*;
import static br.com.hestia.aprendizagem.AcessoCapacitacao.*;
import static br.com.hestia.aprendizagem.ContratoAvaliacao.*;

@Service @Transactional
public class AvaliacaoAprendizagemService {
    final JdbcTemplate jdbc; final AcessoCapacitacao acesso; final ApplicationEventPublisher eventos;
    final ObjectMapper json=new ObjectMapper();
    public AvaliacaoAprendizagemService(JdbcTemplate jdbc,AcessoCapacitacao acesso,ApplicationEventPublisher eventos){this.jdbc=jdbc;this.acesso=acesso;this.eventos=eventos;}
    public JdbcTemplate jdbc(){return jdbc;}
    public AcessoCapacitacao acesso(){return acesso;}
    public ObjectMapper json(){return json;}
    public ApplicationEventPublisher eventos(){return eventos;}
    public Microtreinamento conteudo(Long id){return new MicrotreinamentoFicticioService().criarTreinamentos().stream().filter(t->t.isAtivo()&&t.getId().equals(id)).findFirst().orElseThrow(()->erro(HttpStatus.NOT_FOUND,"Conteúdo ativo indisponível"));}
    public String escrever(Object o){try{return json.writeValueAsString(o);}catch(Exception e){throw new IllegalStateException(e);}}
    public List<Questao> ler(String s){try{return json.readValue(s,new TypeReference<List<Questao>>(){});}catch(Exception e){throw new IllegalStateException("Questionário preservado inválido",e);}}
    public long inserir(String sql,Object... args){var chave=new GeneratedKeyHolder();jdbc.update(c->{var st=c.prepareStatement(sql,Statement.RETURN_GENERATED_KEYS);for(int i=0;i<args.length;i++)st.setObject(i+1,args[i]);return st;},chave);return Objects.requireNonNull(chave.getKeys()).entrySet().stream().filter(e->e.getKey().equalsIgnoreCase("id")).map(e->((Number)e.getValue()).longValue()).findFirst().orElseThrow();}
    public Map<String,Object> catalogo(Long empresa,Long id){return jdbc.queryForList("SELECT * FROM aprendizagem_catalogo WHERE empresa_id=? AND id=?",empresa,id).stream().findFirst().orElseThrow(()->erro(HttpStatus.NOT_FOUND,"Avaliação indisponível"));}
    public static Long id(Map<String,Object> r,String c){return ((Number)r.get(c)).longValue();}
    public long publicar(Usuario autor,Publicacao p){
        acesso.administrador(autor);exigir(p!=null&&p.microtreinamentoId()!=null&&p.versao()>0,HttpStatus.BAD_REQUEST,"Conteúdo e versão obrigatórios");
        var t=conteudo(p.microtreinamentoId());validarQuestoes(p.questoes());acesso.serializar(autor.getId());
        Long empresa=acesso.empresa(autor);
        exigir(jdbc.queryForObject("SELECT count(*) FROM aprendizagem_catalogo WHERE empresa_id=? AND treinamento_id=? AND versao>=?",Long.class,empresa,t.getId(),p.versao())==0,HttpStatus.CONFLICT,"Publique uma versão nova e crescente");
        return inserir("INSERT INTO aprendizagem_catalogo(empresa_id,treinamento_id,versao,conteudo_sha256,tema,questionario,publicado_por,publicado_em) VALUES(?,?,?,?,?,?,?,?)",empresa,t.getId(),p.versao(),hash(t.getConteudo()),t.getTema().name(),escrever(p.questoes()),autor.getId(),LocalDateTime.now());
    }
    public static void validarQuestoes(List<Questao> questoes){
        exigir(questoes!=null&&questoes.size()==5,HttpStatus.BAD_REQUEST,"Publique exatamente cinco questões");Set<String> ids=new HashSet<>();
        for(var q:questoes)exigir(q!=null&&texto(q.id(),80)&&ids.add(q.id())&&texto(q.enunciado(),2000)&&q.alternativas()!=null&&q.alternativas().size()>=3&&q.alternativas().size()<=10&&q.alternativas().entrySet().stream().allMatch(e->texto(e.getKey(),80)&&texto(e.getValue(),2000))&&q.correta()!=null&&q.alternativas().containsKey(q.correta())&&texto(q.explicacao(),3000),HttpStatus.BAD_REQUEST,"Questão incompleta, duplicada ou gabarito inválido");
    }
    static boolean texto(String s,int max){return s!=null&&!s.isBlank()&&s.length()<=max;}
    public List<Map<String,Object>> catalogos(Usuario autor,Long treinamento){
        acesso.empresa(autor);conteudo(treinamento);
        return jdbc.queryForList("SELECT id,treinamento_id,versao,conteudo_sha256,tema,publicado_em FROM aprendizagem_catalogo WHERE empresa_id=? AND treinamento_id=? ORDER BY versao DESC",acesso.empresa(autor),treinamento);
    }
    public Tentativa abrir(Usuario autor,Long catalogoId){
        var u=acesso.usuario(autor,autor.getId());acesso.serializar(u.getId());Long empresa=acesso.empresa(u);var c=catalogo(empresa,catalogoId);
        exigir(hash(conteudo(id(c,"treinamento_id")).getConteudo()).equals(c.get("conteudo_sha256")),HttpStatus.CONFLICT,"Conteúdo mudou: publique uma avaliação compatível");
        exigir(jdbc.queryForObject("SELECT count(*) FROM aprendizagem_leituras WHERE empresa_id=? AND usuario_id=? AND treinamento_id=? AND conteudo_sha256=?",Long.class,empresa,u.getId(),c.get("treinamento_id"),c.get("conteudo_sha256"))>0,HttpStatus.CONFLICT,"Conclua a versão do conteúdo antes de iniciar");
        var ciclos=jdbc.queryForList("SELECT * FROM aprendizagem_ciclos WHERE empresa_id=? AND usuario_id=? AND catalogo_id=? ORDER BY numero DESC",empresa,u.getId(),catalogoId);
        Long ciclo;
        if(ciclos.isEmpty())ciclo=inserir("INSERT INTO aprendizagem_ciclos(empresa_id,usuario_id,catalogo_id,numero,status,motivo,aberto_em) VALUES(?,?,?,1,'EM_ANDAMENTO','PRIMEIRA_AVALIACAO',?)",empresa,u.getId(),catalogoId,LocalDateTime.now());
        else {var ultimo=ciclos.getFirst();exigir("EM_ANDAMENTO".equals(ultimo.get("status")),HttpStatus.CONFLICT,"Ciclo aprovado ou aguardando liberação de reforço");ciclo=id(ultimo,"id");}
        var tentativas=jdbc.queryForList("SELECT id,status FROM aprendizagem_tentativas WHERE ciclo_id=? ORDER BY numero DESC",ciclo);
        if(!tentativas.isEmpty()&&"EM_ANDAMENTO".equals(tentativas.getFirst().get("status")))return consultar(autor,id(tentativas.getFirst(),"id"));
        exigir(tentativas.size()<3,HttpStatus.CONFLICT,"Limite de tentativas atingido");
        long tentativa=inserir("INSERT INTO aprendizagem_tentativas(ciclo_id,numero,aberta_em,questionario_preservado,status) VALUES(?,?,?,?,'EM_ANDAMENTO')",ciclo,tentativas.size()+1,LocalDateTime.now(),c.get("questionario"));
        return consultar(autor,tentativa);
    }
    Map<String,Object> tentativa(Long id){return jdbc.queryForList("SELECT t.*,c.usuario_id,c.empresa_id,c.catalogo_id,a.treinamento_id,a.versao,a.conteudo_sha256 FROM aprendizagem_tentativas t JOIN aprendizagem_ciclos c ON c.id=t.ciclo_id JOIN aprendizagem_catalogo a ON a.id=c.catalogo_id WHERE t.id=?",id).stream().findFirst().orElseThrow(()->erro(HttpStatus.NOT_FOUND,"Tentativa indisponível"));}
    public Tentativa consultar(Usuario autor,Long tentativaId){
        var r=tentativa(tentativaId);acesso.usuario(autor,id(r,"usuario_id"));exigir(acesso.empresa(autor).equals(id(r,"empresa_id")),HttpStatus.NOT_FOUND,"Tentativa indisponível");
        var questoes=ler((String)r.get("questionario_preservado"));boolean finalizada=r.get("finalizada_em")!=null;List<Feedback> feedback=new ArrayList<>();
        if(finalizada){try{List<Resposta> respostas=json.readValue((String)r.get("respostas"),new TypeReference<List<Resposta>>(){});for(var q:questoes){var enviada=respostas.stream().filter(x->q.id().equals(x.questaoId())).findFirst().orElseThrow();feedback.add(new Feedback(q.id(),enviada.alternativa(),q.correta(),q.explicacao()));}}catch(Exception e){throw new IllegalStateException(e);}}
        return new Tentativa(id(r,"id"),id(r,"ciclo_id"),((Number)r.get("numero")).intValue(),id(r,"usuario_id"),id(r,"empresa_id"),id(r,"treinamento_id"),((Number)r.get("versao")).intValue(),(String)r.get("conteudo_sha256"),(String)r.get("status"),r.get("acertos")==null?null:((Number)r.get("acertos")).intValue(),r.get("nota")==null?null:((Number)r.get("nota")).intValue(),questoes.stream().map(q->new QuestaoPublica(q.id(),q.enunciado(),q.alternativas())).toList(),List.copyOf(feedback));
    }
    public Tentativa enviar(Usuario autor,Long tentativaId,Envio envio){
        var r=tentativa(tentativaId);exigir(autor.getId().equals(id(r,"usuario_id")),HttpStatus.FORBIDDEN,"Envie apenas sua própria avaliação");acesso.usuario(autor,autor.getId());acesso.serializar(autor.getId());r=tentativa(tentativaId);
        if(r.get("finalizada_em")!=null)return consultar(autor,tentativaId);
        var qs=ler((String)r.get("questionario_preservado"));int acertos=corrigir(qs,envio);int nota=acertos*20;String status=nota>=80?"APROVADA":"REPROVADA";
        jdbc.update("UPDATE aprendizagem_tentativas SET respostas=?,acertos=?,nota=?,status=?,finalizada_em=? WHERE id=?",escrever(envio.respostas()),acertos,nota,status,LocalDateTime.now(),tentativaId);
        if(nota>=80)jdbc.update("UPDATE aprendizagem_ciclos SET status='APROVADA' WHERE id=?",r.get("ciclo_id"));
        else if(((Number)r.get("numero")).intValue()==3)jdbc.update("UPDATE aprendizagem_ciclos SET status='AGUARDANDO_REFORCO' WHERE id=?",r.get("ciclo_id"));
        eventos.publishEvent(new Atualizada(autor.getId(),acesso.empresa(autor)));return consultar(autor,tentativaId);
    }
    public static int corrigir(List<Questao> qs,Envio envio){
        exigir(envio!=null&&envio.respostas()!=null&&envio.respostas().size()==qs.size(),HttpStatus.BAD_REQUEST,"Responda todas as questões");Map<String,String> respostas=new HashMap<>();
        for(var r:envio.respostas())exigir(r!=null&&r.questaoId()!=null&&r.alternativa()!=null&&respostas.putIfAbsent(r.questaoId(),r.alternativa())==null,HttpStatus.BAD_REQUEST,"Questão duplicada ou resposta inválida");
        int acertos=0;for(var q:qs){String a=respostas.get(q.id());exigir(a!=null&&q.alternativas().containsKey(a),HttpStatus.BAD_REQUEST,"Alternativa inexistente ou questão externa");if(q.correta().equals(a))acertos++;}return acertos;
    }
    public long liberar(Usuario autor,Long cicloId,Liberacao dto){
        Long empresa=acesso.empresa(autor);exigir(autor.getPerfil()==br.com.hestia.usuario.model.PerfilUsuario.ADMIN||jdbc.queryForObject("SELECT count(*) FROM permissoes_capacitacao WHERE empresa_id=? AND usuario_id=?",Long.class,empresa,autor.getId())>0,HttpStatus.FORBIDDEN,"Permissão específica de liberação necessária");
        exigir(dto!=null&&texto(dto.chave(),100)&&texto(dto.justificativa(),1000)&&dto.leiturasReforco()!=null&&!dto.leiturasReforco().isEmpty(),HttpStatus.BAD_REQUEST,"Informe chave, justificativa e evidências de reforço");
        var ciclo=jdbc.queryForList("SELECT * FROM aprendizagem_ciclos WHERE empresa_id=? AND id=?",empresa,cicloId).stream().findFirst().orElseThrow(()->erro(HttpStatus.NOT_FOUND,"Ciclo indisponível"));
        var alvo=acesso.usuario(autor,id(ciclo,"usuario_id"));acesso.serializar(alvo.getId());
        var repetido=jdbc.queryForList("SELECT id,usuario_id,anterior_id FROM aprendizagem_ciclos WHERE empresa_id=? AND chave_liberacao=?",empresa,dto.chave());
        if(!repetido.isEmpty()){var x=repetido.getFirst();exigir(alvo.getId().equals(id(x,"usuario_id"))&&cicloId.equals(id(x,"anterior_id")),HttpStatus.CONFLICT,"Chave já utilizada em outra liberação");return id(x,"id");}
        exigir(!"EM_ANDAMENTO".equals(ciclo.get("status"))&&!"ENCERRADO".equals(ciclo.get("status")),HttpStatus.CONFLICT,"Ciclo não pode ser liberado");
        var c=catalogo(empresa,id(ciclo,"catalogo_id"));
        for(Long leitura:dto.leiturasReforco())exigir(leitura!=null&&jdbc.queryForObject("SELECT count(*) FROM aprendizagem_leituras WHERE id=? AND empresa_id=? AND usuario_id=? AND treinamento_id=? AND conteudo_sha256=? AND concluida_em>=(SELECT max(t.finalizada_em) FROM aprendizagem_tentativas t WHERE t.ciclo_id=?)",Long.class,leitura,empresa,alvo.getId(),c.get("treinamento_id"),c.get("conteudo_sha256"),cicloId)==1,HttpStatus.BAD_REQUEST,"Evidência de reforço inválida");
        jdbc.update("UPDATE aprendizagem_ciclos SET status='ENCERRADO' WHERE id=?",cicloId);
        return inserir("INSERT INTO aprendizagem_ciclos(empresa_id,usuario_id,catalogo_id,numero,status,anterior_id,motivo,autorizado_por,aberto_em,chave_liberacao,evidencias_reforco) VALUES(?,?,?,?,'EM_ANDAMENTO',?,?,?,?,?,?)",empresa,alvo.getId(),c.get("id"),((Number)ciclo.get("numero")).intValue()+1,cicloId,dto.justificativa(),autor.getId(),LocalDateTime.now(),dto.chave(),escrever(dto.leiturasReforco()));
    }
    public List<Map<String,Object>> historico(Usuario autor,Long usuarioId){var u=acesso.usuario(autor,usuarioId);return jdbc.queryForList("SELECT t.id,t.ciclo_id,t.numero,t.status,t.acertos,t.nota,t.aberta_em,t.finalizada_em,c.catalogo_id,c.status AS status_ciclo,a.treinamento_id,a.versao,a.conteudo_sha256 FROM aprendizagem_tentativas t JOIN aprendizagem_ciclos c ON c.id=t.ciclo_id JOIN aprendizagem_catalogo a ON a.id=c.catalogo_id WHERE c.empresa_id=? AND c.usuario_id=? ORDER BY t.id",acesso.empresa(u),u.getId());}
}
