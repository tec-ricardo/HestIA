package br.com.hestia.trilha.service;

import br.com.hestia.aprendizagem.*;
import br.com.hestia.microtreinamento.model.*;
import br.com.hestia.microtreinamento.service.MicrotreinamentoFicticioService;
import br.com.hestia.trilha.model.*;
import br.com.hestia.trilha.repository.*;
import br.com.hestia.usuario.model.*;
import org.springframework.stereotype.Service;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.http.HttpStatus;
import jakarta.persistence.EntityManager;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;
import static br.com.hestia.aprendizagem.AcessoCapacitacao.*;
import static br.com.hestia.aprendizagem.AvaliacaoAprendizagemService.id;

@Service @Transactional
public class MotorTrilhasService {
    private final JdbcTemplate jdbc;private final AcessoCapacitacao acesso;private final br.com.hestia.shared.RepositorioIntegracao aprendizagem;
    private final TrilhaRepository trilhas;private final ParticipacaoTrilhaRepository participacoes;private final EntityManager em;
    public MotorTrilhasService(JdbcTemplate jdbc,AcessoCapacitacao acesso,br.com.hestia.shared.RepositorioIntegracao aprendizagem,TrilhaRepository trilhas,ParticipacaoTrilhaRepository participacoes,EntityManager em){this.jdbc=jdbc;this.acesso=acesso;this.aprendizagem=aprendizagem;this.trilhas=trilhas;this.participacoes=participacoes;this.em=em;}
    public record ConformidadeRegistrada(Long usuarioId,Long empresaId){}
    record Evento(String chave,String origem,TipoErro erro,TemaTreinamento tema,LocalDateTime data){}
    public record Confirmacao(Long avaliacaoId,TipoErro tipoErro,String justificativa){}
    public long confirmar(Usuario autor,Confirmacao d){
        exigir(autor.getPerfil()!=PerfilUsuario.FUNCIONARIO,HttpStatus.FORBIDDEN,"Confirmação exige responsável autorizado");
        exigir(d!=null&&d.avaliacaoId()!=null&&d.tipoErro()!=null&&d.justificativa()!=null&&!d.justificativa().isBlank()&&d.justificativa().length()<=1000,HttpStatus.BAD_REQUEST,"Identifique critério, avaliação e justificativa");
        Long empresa=acesso.empresa(autor);var r=jdbc.queryForList("SELECT a.id,r.usuario_id,r.data_hora,a.status FROM avaliacoes_conformidade a JOIN registros_uso_ia r ON r.id=a.registro_uso_id JOIN usuarios u ON u.id=r.usuario_id WHERE a.id=? AND u.empresa_id=?",d.avaliacaoId(),empresa).stream().findFirst().orElseThrow(()->erro(HttpStatus.NOT_FOUND,"Avaliação indisponível"));
        acesso.usuario(autor,id(r,"usuario_id"));acesso.serializar(id(r,"usuario_id"));exigir(!"CONFORME".equals(r.get("status")),HttpStatus.CONFLICT,"Avaliação sem problema pendente");
        String criterio=switch(d.tipoErro()){case FERRAMENTA_NAO_AUTORIZADA->"IA_AUTORIZADA";case VIOLACAO_POLITICA_INTERNA->"POLITICA_INTERNA";case FINALIDADE_INADEQUADA->"FINALIDADE_ADEQUADA";case AUSENCIA_VALIDACAO_HUMANA->"VALIDACAO_HUMANA";case USO_NAO_REGISTRADO->"REGISTRO_DE_USO";case VIOLACAO_LGPD->"LGPD";case DADOS_SENSIVEIS->"DADOS_SENSIVEIS";};
        exigir(jdbc.queryForObject("SELECT count(*) FROM avaliacao_criterios_atendidos WHERE avaliacao_id=? AND criterio=?",Long.class,d.avaliacaoId(),criterio)==0,HttpStatus.CONFLICT,"O critério foi atendido nesta avaliação");
        var existente=jdbc.queryForList("SELECT id FROM trilha_ocorrencias_confirmadas WHERE empresa_id=? AND avaliacao_id=? AND tipo_erro=?",empresa,d.avaliacaoId(),d.tipoErro().name());
        Long origem=existente.isEmpty()?aprendizagem.inserir("INSERT INTO trilha_ocorrencias_confirmadas(empresa_id,usuario_id,avaliacao_id,tipo_erro,ocorrido_em,confirmado_por,confirmado_em,justificativa) VALUES(?,?,?,?,?,?,?,?)",empresa,r.get("usuario_id"),d.avaliacaoId(),d.tipoErro().name(),r.get("data_hora"),autor.getId(),LocalDateTime.now(),d.justificativa()):id(existente.getFirst(),"id");
        processar(autor,id(r,"usuario_id"));return origem;
    }
    @TransactionalEventListener(phase=TransactionPhase.BEFORE_COMMIT)
    public void conformidade(ConformidadeRegistrada e){processarInterno(e.usuarioId(),e.empresaId());}
    @TransactionalEventListener(phase=TransactionPhase.BEFORE_COMMIT)
    @org.springframework.core.annotation.Order(50)
    public void aprendizagem(ContratoAvaliacao.Atualizada e){processarInterno(e.usuarioId(),e.empresaId());}
    public void processar(Usuario autor,Long usuario){var u=acesso.usuario(autor,usuario);processarInterno(u.getId(),acesso.empresa(u));}
    void processarInterno(Long usuario,Long empresa){
        acesso.serializar(usuario);em.flush();
        if(jdbc.queryForObject("SELECT count(*) FROM usuarios WHERE id=? AND empresa_id=? AND ativo=true",Long.class,usuario,empresa)==0)return;
        LocalDateTime agora=LocalDateTime.now();List<Evento> eventos=new ArrayList<>();
        for(var r:jdbc.queryForList("SELECT a.id,r.data_hora,a.status,EXISTS(SELECT 1 FROM avaliacao_criterios_atendidos c WHERE c.avaliacao_id=a.id AND c.criterio='IA_AUTORIZADA') AS ia,EXISTS(SELECT 1 FROM avaliacao_criterios_atendidos c WHERE c.avaliacao_id=a.id AND c.criterio='POLITICA_INTERNA') AS politica FROM avaliacoes_conformidade a JOIN registros_uso_ia r ON r.id=a.registro_uso_id JOIN usuarios u ON u.id=r.usuario_id JOIN politica_uso_versoes v ON v.id=a.politica_versao_id AND v.empresa_id=u.empresa_id AND v.politica_uso_id=a.politica_uso_id WHERE u.empresa_id=? AND u.id=? AND a.status='NAO_CONFORME' AND a.regra_codigo='CONFORMIDADE_CRITERIOS' AND a.regra_versao='1.0' AND r.data_hora>=? AND r.data_hora<=?",empresa,usuario,agora.minusDays(30),agora)){
            if(Boolean.FALSE.equals(r.get("ia")))eventos.add(eventoAvaliacao(id(r,"id"),TipoErro.FERRAMENTA_NAO_AUTORIZADA,((Timestamp)r.get("data_hora")).toLocalDateTime()));
            if(Boolean.FALSE.equals(r.get("politica")))eventos.add(eventoAvaliacao(id(r,"id"),TipoErro.VIOLACAO_POLITICA_INTERNA,((Timestamp)r.get("data_hora")).toLocalDateTime()));
        }
        for(var r:jdbc.queryForList("SELECT avaliacao_id,tipo_erro,ocorrido_em FROM trilha_ocorrencias_confirmadas WHERE empresa_id=? AND usuario_id=? AND ocorrido_em>=? AND ocorrido_em<=?",empresa,usuario,agora.minusDays(30),agora))eventos.add(eventoAvaliacao(id(r,"avaliacao_id"),TipoErro.valueOf((String)r.get("tipo_erro")),((Timestamp)r.get("ocorrido_em")).toLocalDateTime()));
        for(var r:jdbc.queryForList("SELECT t.id,t.finalizada_em,a.tema FROM aprendizagem_tentativas t JOIN aprendizagem_ciclos c ON c.id=t.ciclo_id JOIN aprendizagem_catalogo a ON a.id=c.catalogo_id WHERE c.empresa_id=? AND c.usuario_id=? AND t.status='REPROVADA' AND t.finalizada_em<=?",empresa,usuario,agora))eventos.add(new Evento("APRENDIZAGEM:"+r.get("id"),"AVALIACAO_REPROVADA",null,TemaTreinamento.valueOf((String)r.get("tema")),((Timestamp)r.get("finalizada_em")).toLocalDateTime()));
        eventos=new ArrayList<>(eventos.stream().collect(java.util.stream.Collectors.toMap(Evento::chave,x->x,(x,y)->x,LinkedHashMap::new)).values());
        for(var tema:TemaTreinamento.values()){
            var candidatos=eventos.stream().filter(e->e.tema()==tema).toList();if(candidatos.isEmpty())continue;
            var anteriores=jdbc.queryForList("SELECT c.*,p.status,p.concluida_em FROM trilha_ciclos_automaticos c JOIN participacoes_trilha p ON p.id=c.participacao_id WHERE c.empresa_id=? AND c.usuario_id=? AND c.tema=? ORDER BY c.id DESC",empresa,usuario,tema.name());
            Map<String,Object> anterior=anteriores.isEmpty()?null:anteriores.getFirst();
            LocalDateTime corte=anterior!=null&&"CONCLUIDA".equals(anterior.get("status"))&&anterior.get("concluida_em")!=null?((Timestamp)anterior.get("concluida_em")).toLocalDateTime():null;
            var novos=candidatos.stream().filter(e->corte==null||e.data().isAfter(corte)).filter(e->jdbc.queryForObject("SELECT count(*) FROM trilha_evidencias WHERE empresa_id=? AND usuario_id=? AND chave_origem=?",Long.class,empresa,usuario,e.chave())==0).toList();
            if(novos.isEmpty())continue;
            boolean imediato=novos.stream().anyMatch(e->e.erro()==null||Set.of(TipoErro.DADOS_SENSIVEIS,TipoErro.FERRAMENTA_NAO_AUTORIZADA,TipoErro.VIOLACAO_POLITICA_INTERNA,TipoErro.VIOLACAO_LGPD).contains(e.erro()));
            boolean recorrente=novos.stream().filter(e->e.erro()!=null).collect(java.util.stream.Collectors.groupingBy(Evento::erro,java.util.stream.Collectors.counting())).values().stream().anyMatch(n->n>=3);
            boolean reincidenciaPosConclusao=corte!=null&&!novos.isEmpty();
            boolean ativo=anterior!=null&&!"CONCLUIDA".equals(anterior.get("status"));
            if(!ativo&&!imediato&&!recorrente&&!reincidenciaPosConclusao)continue;
            Long ciclo;
            if(ativo)ciclo=id(anterior,"id");
            else {
                var treinamentos=new MicrotreinamentoFicticioService().criarTreinamentos().stream().filter(t->t.isAtivo()&&t.getTema()==tema).toList();List<Map<String,Object>> requisitos=new ArrayList<>();boolean completo=!treinamentos.isEmpty();
                for(var t:treinamentos){var avaliacoes=jdbc.queryForList("SELECT id,treinamento_id,conteudo_sha256 FROM aprendizagem_catalogo WHERE empresa_id=? AND treinamento_id=? AND conteudo_sha256=? ORDER BY versao DESC",empresa,t.getId(),hash(t.getConteudo()));if(avaliacoes.isEmpty()){completo=false;break;}requisitos.add(avaliacoes.getFirst());}
                if(!completo){jdbc.update("DELETE FROM trilha_pendencias_catalogo WHERE empresa_id=? AND usuario_id=? AND tema=?",empresa,usuario,tema.name());jdbc.update("INSERT INTO trilha_pendencias_catalogo VALUES(?,?,?,?,?)",empresa,usuario,tema.name(),"Conteúdo ativo ou avaliação publicada compatível ausente",agora);continue;}
                var trilha=new Trilha(empresa,"Capacitação: "+tema.name(),treinamentos.stream().map(Microtreinamento::getId).toList());trilha.setExigeAprovacao(true);trilhas.saveAndFlush(trilha);var participacao=participacoes.saveAndFlush(new ParticipacaoTrilha(trilha,usuario));
                boolean corretiva=novos.stream().anyMatch(e->e.erro()!=null);ciclo=aprendizagem.inserir("INSERT INTO trilha_ciclos_automaticos(empresa_id,usuario_id,participacao_id,tema,tipo,regra_versao,anterior_id,corte_aprendizagem,motivo,criado_em) VALUES(?,?,?,?,?,?,?,?,?,?)",empresa,usuario,participacao.getId(),tema.name(),corretiva?"CORRETIVA":"ADAPTATIVA","SCRUM-488-proposta-1",anterior==null?null:anterior.get("id"),corte,"Evidências confirmadas: "+novos.size(),agora);
                for(var req:requisitos)jdbc.update("INSERT INTO trilha_aprendizagem_requisitos VALUES(?,?,?,?)",trilha.getId(),req.get("treinamento_id"),req.get("id"),req.get("conteudo_sha256"));
                jdbc.update("DELETE FROM trilha_pendencias_catalogo WHERE empresa_id=? AND usuario_id=? AND tema=?",empresa,usuario,tema.name());
            }
            for(var e:novos)jdbc.update("INSERT INTO trilha_evidencias(ciclo_id,empresa_id,usuario_id,chave_origem,tipo_origem,ocorrido_em,processado_em) VALUES(?,?,?,?,?,?,?)",ciclo,empresa,usuario,e.chave(),e.origem(),e.data(),agora);
        }
    }
    private Evento eventoAvaliacao(Long id,TipoErro tipo,LocalDateTime data){return new Evento("CONFORMIDADE:"+id+":"+tipo.name(),"CONFORMIDADE_CONFIRMADA",tipo,tipo.getTemaTreinamento(),data);}
    public List<Map<String,Object>> ciclos(Usuario autor,Long usuario){var u=acesso.usuario(autor,usuario);return jdbc.queryForList("SELECT c.*,p.status,p.concluida_em FROM trilha_ciclos_automaticos c JOIN participacoes_trilha p ON p.id=c.participacao_id WHERE c.empresa_id=? AND c.usuario_id=? ORDER BY c.id",acesso.empresa(u),usuario);}
    public List<Map<String,Object>> pendencias(Usuario autor,Long usuario){var u=acesso.usuario(autor,usuario);return jdbc.queryForList("SELECT tema,motivo,identificada_em FROM trilha_pendencias_catalogo WHERE empresa_id=? AND usuario_id=?",acesso.empresa(u),usuario);}
}
