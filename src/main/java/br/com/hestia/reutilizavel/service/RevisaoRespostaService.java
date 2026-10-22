package br.com.hestia.reutilizavel.service;
import br.com.hestia.aprendizagem.*;
import br.com.hestia.reutilizavel.model.*;
import br.com.hestia.reutilizavel.repository.RespostaReutilizavelRepository;
import br.com.hestia.usuario.model.Usuario;
import jakarta.persistence.*;
import org.springframework.stereotype.Service;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import java.time.LocalDateTime;
import java.util.*;
import static br.com.hestia.aprendizagem.AcessoCapacitacao.*;
@Service @Transactional
public class RevisaoRespostaService {
    private final AcessoCapacitacao acesso;private final JdbcTemplate jdbc;private final EntityManager em;private final RespostaReutilizavelRepository repository;
    public RevisaoRespostaService(AcessoCapacitacao acesso,JdbcTemplate jdbc,EntityManager em,RespostaReutilizavelRepository repository){this.acesso=acesso;this.jdbc=jdbc;this.em=em;this.repository=repository;}
    public record Acao(StatusQualidadeResposta status,String justificativa,LocalDateTime expiraEm){}
    public List<Map<String,Object>> listar(Usuario u,int pagina,int tamanho){acesso.administrador(u);exigir(pagina>=0&&pagina<=10000&&tamanho>=1&&tamanho<=100,HttpStatus.BAD_REQUEST,"Paginação inválida");return jdbc.queryForList("SELECT id,prompt_original,categoria,status_qualidade,expira_em,data_criacao,quantidade_reutilizacoes FROM respostas_reutilizaveis WHERE empresa_id=? ORDER BY id DESC LIMIT ? OFFSET ?",acesso.empresa(u),tamanho,pagina*tamanho);}
    private RespostaReutilizavel obter(Usuario u,Long id,boolean lock){acesso.administrador(u);var r=lock?em.find(RespostaReutilizavel.class,id,LockModeType.PESSIMISTIC_WRITE):repository.findById(id).orElse(null);exigir(r!=null&&acesso.empresa(u).equals(r.getEmpresaId()),HttpStatus.NOT_FOUND,"Resposta indisponível");return r;}
    public Map<String,Object> detalhe(Usuario u,Long id){var r=obter(u,id,false);return Map.of("resposta",r,"revisoes",jdbc.queryForList("SELECT * FROM resposta_revisoes WHERE empresa_id=? AND resposta_id=? ORDER BY id DESC",acesso.empresa(u),id),"versoes",jdbc.queryForList("SELECT id,numero_versao,origem,tipo_conteudo,conteudo_sha256,criada_em FROM resposta_versoes WHERE resposta_id=? ORDER BY numero_versao DESC",id));}
    public RespostaReutilizavel revisar(Usuario u,Long id,Acao a){
        exigir(a!=null&&a.status()!=null&&a.justificativa()!=null&&!a.justificativa().isBlank()&&a.justificativa().length()<=1000,HttpStatus.BAD_REQUEST,"Status e justificativa obrigatórios");var r=obter(u,id,true);var anterior=r.getStatusQualidade();
        exigir(anterior!=StatusQualidadeResposta.RETIRADA||a.status()==StatusQualidadeResposta.RETIRADA,HttpStatus.CONFLICT,"Conteúdo retirado exige nova publicação");
        if(a.status()==StatusQualidadeResposta.APROVADA)exigir(Boolean.FALSE.equals(r.getPossuiDadosSensiveis())&&!r.getPerfisPermitidos().isEmpty()&&r.getPromptOriginal()!=null&&!r.getPromptOriginal().isBlank()&&r.getRespostaGerada()!=null&&!r.getRespostaGerada().isBlank()&&(a.expiraEm()==null||a.expiraEm().isAfter(LocalDateTime.now())),HttpStatus.UNPROCESSABLE_ENTITY,"Conteúdo, perfis ou validade impedem aprovação");
        if(anterior==a.status()&&Objects.equals(r.getExpiraEm(),a.expiraEm()))return r;
        r.setStatusQualidade(a.status());r.setExpiraEm(a.expiraEm());r.setReutilizavel(a.status()==StatusQualidadeResposta.APROVADA);
        if(a.status()!=StatusQualidadeResposta.APROVADA)r.setEmbedding(null);
        repository.saveAndFlush(r);
        if(a.status()!=StatusQualidadeResposta.APROVADA&&"PostgreSQL".equalsIgnoreCase(jdbc.execute((org.springframework.jdbc.core.ConnectionCallback<String>)c->c.getMetaData().getDatabaseProductName())))jdbc.update("UPDATE respostas_reutilizaveis SET embedding_vector=NULL WHERE id=? AND empresa_id=?",id,acesso.empresa(u));
        Integer versao=jdbc.queryForObject("SELECT coalesce(max(numero_versao),0) FROM resposta_versoes WHERE resposta_id=?",Integer.class,id);
        jdbc.update("INSERT INTO resposta_revisoes(resposta_id,empresa_id,responsavel_id,estado_anterior,estado_atual,justificativa,revisada_em,conteudo_sha256,versao_conteudo,expira_em) VALUES(?,?,?,?,?,?,?,?,?,?)",id,acesso.empresa(u),u.getId(),anterior.name(),a.status().name(),a.justificativa(),LocalDateTime.now(),hash(r.getPromptOriginal()+"\n"+r.getRespostaGerada()),versao,a.expiraEm());return r;
    }
}
