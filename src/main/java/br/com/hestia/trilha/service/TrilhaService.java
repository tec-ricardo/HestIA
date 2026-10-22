package br.com.hestia.trilha.service;
import br.com.hestia.trilha.model.*;
import br.com.hestia.trilha.repository.*;
import br.com.hestia.usuario.model.*;
import br.com.hestia.usuario.repository.UsuarioRepository;
import br.com.hestia.microtreinamento.model.ConclusaoMicrotreinamento;
import br.com.hestia.microtreinamento.repository.ConclusaoMicrotreinamentoRepository;
import br.com.hestia.microtreinamento.service.MicrotreinamentoFicticioService;
import br.com.hestia.reutilizavel.service.AcessoRespostaService;
import jakarta.persistence.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;
import java.time.LocalDateTime;

@Service @Transactional
public class TrilhaService {
    @org.springframework.transaction.event.TransactionalEventListener(phase=org.springframework.transaction.event.TransactionPhase.BEFORE_COMMIT)
    @org.springframework.core.annotation.Order(200)
    public void sincronizarAprendizagem(br.com.hestia.aprendizagem.ContratoAvaliacao.Atualizada evento){usuarios.findById(evento.usuarioId()).filter(u->u.getEmpresa()!=null&&evento.empresaId().equals(u.getEmpresa().getId())&&Boolean.TRUE.equals(u.getAtivo())).ifPresent(u->historico(u,u.getId()));}
    @org.springframework.beans.factory.annotation.Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbc;
    private final TrilhaRepository trilhas; private final ParticipacaoTrilhaRepository participacoes;
    private final ConclusaoMicrotreinamentoRepository conclusoes; private final UsuarioRepository usuarios;
    private final AcessoRespostaService acesso; private final EntityManager em;
    public TrilhaService(TrilhaRepository trilhas,ParticipacaoTrilhaRepository participacoes,
            ConclusaoMicrotreinamentoRepository conclusoes,UsuarioRepository usuarios,AcessoRespostaService acesso,EntityManager em){
        this.trilhas=trilhas;this.participacoes=participacoes;this.conclusoes=conclusoes;
        this.usuarios=usuarios;this.acesso=acesso;this.em=em;
    }
    public record Definicao(Long id,Long empresaId,String titulo,List<Long> microtreinamentos){}
    public record Etapa(int ordem,Long microtreinamentoId,Long conclusaoId,LocalDateTime dataConclusao,String status){}
    public record Progresso(Long id,Long trilhaId,String titulo,Long usuarioId,Long empresaId,
            String status,int totalEtapas,int etapasConcluidas,double percentual,
            LocalDateTime iniciadaEm,LocalDateTime dataConclusao,List<Etapa> etapas){}
    public Definicao criar(Usuario autor,String titulo,List<Long> etapas){
        Long empresa=acesso.validarEmpresa(autor,null);
        if(autor.getPerfil()==PerfilUsuario.FUNCIONARIO) throw erro(HttpStatus.FORBIDDEN,"Somente gestores e administradores criam trilhas");
        if(titulo==null||titulo.isBlank()||titulo.length()>255||etapas==null||etapas.isEmpty()||etapas.size()>100
                ||etapas.stream().anyMatch(Objects::isNull)||new HashSet<>(etapas).size()!=etapas.size())
            throw erro(HttpStatus.BAD_REQUEST,"Título e etapas distintas são obrigatórios");
        var ativos=new MicrotreinamentoFicticioService().criarTreinamentos().stream().filter(t->t.isAtivo()).map(t->t.getId()).toList();
        if(!ativos.containsAll(etapas)) throw erro(HttpStatus.BAD_REQUEST,"Microtreinamento não existe no catálogo ativo");
        return definicao(trilhas.save(new Trilha(empresa,titulo.trim(),etapas)));
    }
    @Transactional(readOnly=true)
    public List<Definicao> listar(Usuario autor){return trilhas.findByEmpresaIdOrderByIdAsc(acesso.validarEmpresa(autor,null)).stream().map(this::definicao).toList();}
    private Definicao definicao(Trilha t){return new Definicao(t.getId(),t.getEmpresaId(),t.getTitulo(),t.getEtapas());}
    private Usuario participante(Usuario autor,Long id){
        Long empresa=acesso.validarEmpresa(autor,null);
        if(id==null||id<=0) throw erro(HttpStatus.BAD_REQUEST,"Usuário inválido");
        var u=usuarios.findById(id).filter(x->x.getEmpresa()!=null&&empresa.equals(x.getEmpresa().getId())&&Boolean.TRUE.equals(x.getAtivo()))
                .orElseThrow(()->erro(HttpStatus.NOT_FOUND,"Usuário indisponível"));
        if(autor.getPerfil()==PerfilUsuario.FUNCIONARIO&&!autor.getId().equals(id)) throw erro(HttpStatus.FORBIDDEN,"Acesso restrito ao próprio progresso");
        if(autor.getPerfil()==PerfilUsuario.GESTOR&&(autor.getDepartamento()==null||u.getDepartamento()==null||!autor.getDepartamento().getId().equals(u.getDepartamento().getId())))throw erro(HttpStatus.FORBIDDEN,"Departamento não autorizado");
        return u;
    }
    public Progresso iniciar(Usuario autor,Long trilhaId,Long usuarioId){
        var u=participante(autor,usuarioId);
        em.lock(u,LockModeType.PESSIMISTIC_WRITE);
        var t=trilhas.findById(trilhaId).filter(x->x.getEmpresaId().equals(u.getEmpresa().getId()))
                .orElseThrow(()->erro(HttpStatus.NOT_FOUND,"Trilha indisponível"));
        var p=participacoes.findByTrilhaIdAndUsuarioId(trilhaId,usuarioId).orElseGet(()->participacoes.save(new ParticipacaoTrilha(t,usuarioId)));
        return atualizar(p);
    }
    public List<Progresso> historico(Usuario autor,Long usuarioId){
        var u=participante(autor,usuarioId);em.lock(u,LockModeType.PESSIMISTIC_WRITE);
        return participacoes.findByUsuarioIdAndEmpresaIdOrderByIdAsc(usuarioId,u.getEmpresa().getId()).stream().map(this::atualizar).toList();
    }
    public Progresso consultar(Usuario autor,Long id){
        var p=participacoes.findById(id).filter(x->x.getEmpresaId().equals(acesso.validarEmpresa(autor,null)))
                .orElseThrow(()->erro(HttpStatus.NOT_FOUND,"Participação indisponível"));
        var u=participante(autor,p.getUsuarioId());em.lock(u,LockModeType.PESSIMISTIC_WRITE);
        return atualizar(p);
    }
    public List<Progresso> porConclusao(Usuario autor,Long conclusaoId){
        var c=conclusoes.findById(conclusaoId).orElseThrow(()->erro(HttpStatus.NOT_FOUND,"Conclusão indisponível"));
        return historico(autor,c.getUsuarioId()).stream().filter(p->p.etapas().stream().anyMatch(e->conclusaoId.equals(e.conclusaoId()))).toList();
    }
    private Progresso atualizar(ParticipacaoTrilha p){
        Map<Long,ConclusaoMicrotreinamento> existentes=new HashMap<>();
        for(var c:conclusoes.findByUsuarioId(p.getUsuarioId())) {
            if(c.isConcluido()&&c.getDataConclusao()!=null&&!c.getDataConclusao().isAfter(LocalDateTime.now()))
                existentes.put(c.getMicrotreinamentoId(),c);
        }
        Map<Long,Long> vinculos=new HashMap<>();List<Etapa> etapas=new ArrayList<>();LocalDateTime fim=null;
        for(Long id:p.getTrilha().getEtapas()){
            var c=existentes.get(id);boolean aprendizagem=!p.getTrilha().isExigeAprovacao();LocalDateTime aprovadaEm=null;
            if(!aprendizagem){
                var provas=jdbc.queryForList("SELECT max(t.finalizada_em) AS aprovada_em FROM trilha_aprendizagem_requisitos req JOIN aprendizagem_catalogo a ON a.id=req.catalogo_id JOIN aprendizagem_ciclos ac ON ac.catalogo_id=a.id JOIN aprendizagem_tentativas t ON t.ciclo_id=ac.id JOIN aprendizagem_leituras l ON l.empresa_id=ac.empresa_id AND l.usuario_id=ac.usuario_id AND l.treinamento_id=req.treinamento_id AND l.conteudo_sha256=req.conteudo_sha256 JOIN trilha_ciclos_automaticos tc ON tc.participacao_id=? WHERE req.trilha_id=? AND req.treinamento_id=? AND ac.empresa_id=? AND ac.usuario_id=? AND t.status='APROVADA' AND (tc.corte_aprendizagem IS NULL OR (t.finalizada_em>tc.corte_aprendizagem AND l.concluida_em>tc.corte_aprendizagem))",p.getId(),p.getTrilha().getId(),id,p.getEmpresaId(),p.getUsuarioId());
                Object data=provas.getFirst().get("aprovada_em");if(data instanceof java.sql.Timestamp ts){aprendizagem=true;aprovadaEm=ts.toLocalDateTime();}
            }
            if(c!=null&&aprendizagem){vinculos.put(id,c.getId());var data=aprovadaEm!=null&&aprovadaEm.isAfter(c.getDataConclusao())?aprovadaEm:c.getDataConclusao();if(fim==null||data.isAfter(fim))fim=data;}
            etapas.add(new Etapa(etapas.size()+1,id,c==null?null:c.getId(),c==null?null:c.getDataConclusao(),c==null?"PENDENTE":aprendizagem?"CONCLUIDA":"AGUARDANDO_APROVACAO"));
        }
        p.sincronizar(vinculos,fim,etapas.stream().anyMatch(e->e.conclusaoId()!=null));participacoes.save(p);
        return new Progresso(p.getId(),p.getTrilha().getId(),p.getTrilha().getTitulo(),p.getUsuarioId(),p.getEmpresaId(),p.getStatus().name(),
            etapas.size(),vinculos.size(),100.0*vinculos.size()/etapas.size(),p.getIniciadaEm(),p.getConcluidaEm(),List.copyOf(etapas));
    }
    private ResponseStatusException erro(HttpStatus s,String m){return new ResponseStatusException(s,m);}
}
