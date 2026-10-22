package br.com.hestia.rag.service;
import br.com.hestia.aprendizagem.*;
import br.com.hestia.reutilizavel.service.*;
import br.com.hestia.reutilizavel.model.RespostaReutilizavel;
import br.com.hestia.usuario.model.Usuario;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Value;
import br.com.hestia.registro.service.RegistroUsoIAService;
import br.com.hestia.registro.dto.RegistroUsoIACriacaoDTO;
import br.com.hestia.registro.model.*;
import java.time.LocalDateTime;
import java.util.*;
import static br.com.hestia.aprendizagem.AcessoCapacitacao.*;
import static br.com.hestia.aprendizagem.AvaliacaoAprendizagemService.id;
@Service @Transactional
public class InteracaoRagService {
    public enum Modo { CONSULTAR, GERAR_NOVA, REUTILIZAR }
    public record Pergunta(String chave,String pergunta,Modo modo,Long respostaId){}
    public record Fonte(Long id,String titulo,String trecho,Integer versao,String conteudoSha256){}
    public record Resultado(Long id,String pergunta,LocalDateTime criadaEm,String estado,String resposta,List<Fonte> fontes,Double similaridade,String confianca,String natureza,String aviso){}
    private final br.com.hestia.shared.RepositorioIntegracao db;private final BuscaVetorialService busca;private final AcessoRespostaService acesso;
    private final GeracaoRagService geracao;private final RespostaReutilizavelService respostas;private final RegistroUsoIAService registros;private final Long ferramentaId;
    public InteracaoRagService(br.com.hestia.shared.RepositorioIntegracao db,BuscaVetorialService busca,AcessoRespostaService acesso,GeracaoRagService geracao,RespostaReutilizavelService respostas,RegistroUsoIAService registros,@Value("${hestia.rag.ferramenta-id:0}")Long ferramentaId){this.db=db;this.busca=busca;this.acesso=acesso;this.geracao=geracao;this.respostas=respostas;this.registros=registros;this.ferramentaId=ferramentaId;}
    public Resultado perguntar(Usuario u,Pergunta p){
        Long empresa=acesso.validarEmpresa(u,null);exigir(p!=null&&p.chave()!=null&&!p.chave().isBlank()&&p.chave().length()<=100&&p.pergunta()!=null&&!p.pergunta().isBlank()&&p.pergunta().length()<=4000&&p.modo()!=null,HttpStatus.BAD_REQUEST,"Pergunta, modo e chave obrigatórios");db.acesso().serializar(u.getId());
        var anteriores=db.jdbc().queryForList("SELECT * FROM interacoes_rag WHERE empresa_id=? AND usuario_id=? AND chave=?",empresa,u.getId(),p.chave());
        if(!anteriores.isEmpty()){var r=anteriores.getFirst();exigir(p.pergunta().equals(r.get("pergunta"))&&p.modo().name().equals(r.get("modo")),HttpStatus.CONFLICT,"Chave já utilizada para outra interação");return ler(u,r);}
        List<RespostaReutilizavel> candidatas;
        if(p.modo()==Modo.REUTILIZAR){exigir(p.respostaId()!=null,HttpStatus.BAD_REQUEST,"Resposta para reutilização obrigatória");candidatas=List.of(acesso.buscarPorId(u,p.respostaId()));}
        else {try{candidatas=busca.buscarSemelhantes(empresa,p.pergunta(),5);}catch(org.springframework.web.server.ResponseStatusException e){throw e;}catch(RuntimeException e){throw erro(HttpStatus.SERVICE_UNAVAILABLE,"Recuperação vetorial indisponível");}}
        // Revalidação após a busca: somente estes trechos entram no prompt de geração.
        List<Fonte> fontes=new ArrayList<>();int caracteres=0;
        for(var candidata:candidatas){var r=acesso.buscarPorId(u,candidata.getId());String trecho=r.getRespostaGerada();if(trecho.length()>6000)trecho=trecho.substring(0,6000);if(caracteres+trecho.length()>18000)break;caracteres+=trecho.length();var versoes=db.jdbc().queryForList("SELECT numero_versao,conteudo_sha256 FROM resposta_versoes WHERE resposta_id=? ORDER BY numero_versao DESC",r.getId());fontes.add(new Fonte(r.getId(),r.getPromptOriginal(),trecho,versoes.isEmpty()?null:((Number)versoes.getFirst().get("numero_versao")).intValue(),versoes.isEmpty()?hash(r.getPromptOriginal()+"\n"+r.getRespostaGerada()):(String)versoes.getFirst().get("conteudo_sha256")));}
        String estado=fontes.isEmpty()?"SEM_CONTEUDO_AUTORIZADO":"RECUPERADA";String texto=fontes.isEmpty()?null:fontes.getFirst().trecho();String modelo=null;Long entrada=null,saida=null,registro=null;
        if(!fontes.isEmpty()&&p.modo()==Modo.GERAR_NOVA){var resposta=geracao.gerar(p.pergunta(),fontes.stream().map(f->"[Fonte "+f.id()+"] "+f.trecho()).toList());texto=resposta.texto();modelo=resposta.modelo();entrada=resposta.tokensEntrada();saida=resposta.tokensSaida();estado="GERADA_COM_FONTES";
            if(ferramentaId>0&&entrada!=null&&saida!=null&&entrada>=0&&saida>=0&&entrada<=Integer.MAX_VALUE&&saida<=Integer.MAX_VALUE&&entrada+saida<=Integer.MAX_VALUE&&db.jdbc().queryForObject("SELECT count(*) FROM ferramentas_ia WHERE id=? AND empresa_id=? AND status='APROVADA'",Long.class,ferramentaId,empresa)==1)registro=registros.criarCapturado(new RegistroUsoIACriacaoDTO(u.getId(),ferramentaId,modelo,LocalDateTime.now(),FinalidadeUso.GERACAO_CONTEUDO,entrada.intValue(),saida.intValue(),null,StatusExecucao.SUCESSO,null),"RAG:"+u.getId()+":"+p.chave()).id();
        }
        if(!fontes.isEmpty()&&p.modo()==Modo.REUTILIZAR){respostas.registrarReutilizacao(fontes.getFirst().id());estado="REUTILIZADA";}
        Long nova=db.inserir("INSERT INTO interacoes_rag(empresa_id,usuario_id,chave,pergunta,modo,estado,resposta,fontes,modelo,tokens_entrada,tokens_saida,registro_uso_id,criada_em) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?)",empresa,u.getId(),p.chave(),p.pergunta(),p.modo().name(),estado,texto,db.escrever(fontes),modelo,entrada,saida,registro,LocalDateTime.now());
        return ler(u,db.jdbc().queryForList("SELECT * FROM interacoes_rag WHERE id=? AND empresa_id=? AND usuario_id=?",nova,empresa,u.getId()).getFirst());
    }
    Resultado ler(Usuario u,Map<String,Object> row){
        List<Fonte> fontes;try{fontes=db.json().readValue((String)row.get("fontes"),new com.fasterxml.jackson.core.type.TypeReference<List<Fonte>>(){});}catch(Exception e){throw new IllegalStateException(e);}
        for(var f:fontes){try{var atual=acesso.buscarPorId(u,f.id());String atualHash=hash(atual.getPromptOriginal()+"\n"+atual.getRespostaGerada());var versoes=db.jdbc().queryForList("SELECT conteudo_sha256 FROM resposta_versoes WHERE resposta_id=? ORDER BY numero_versao DESC",f.id());if(!versoes.isEmpty())atualHash=(String)versoes.getFirst().get("conteudo_sha256");if(!atualHash.equals(f.conteudoSha256()))throw erro(HttpStatus.NOT_FOUND,"Fonte alterada");}catch(org.springframework.web.server.ResponseStatusException e){return new Resultado(id(row,"id"),(String)row.get("pergunta"),((java.sql.Timestamp)row.get("criada_em")).toLocalDateTime(),"FONTES_INDISPONIVEIS",null,List.of(),null,"NAO_CLASSIFICADA","PLATAFORMA","Fonte alterada, retirada, expirada ou sem permissão atual");}}
        return new Resultado(id(row,"id"),(String)row.get("pergunta"),((java.sql.Timestamp)row.get("criada_em")).toLocalDateTime(),(String)row.get("estado"),(String)row.get("resposta"),fontes,null,"NAO_CLASSIFICADA","PLATAFORMA","Histórico restrito ao autor e revalidado no acesso");
    }
    public List<Resultado> historico(Usuario u){Long empresa=acesso.validarEmpresa(u,null);return db.jdbc().queryForList("SELECT * FROM interacoes_rag WHERE empresa_id=? AND usuario_id=? ORDER BY id DESC LIMIT 100",empresa,u.getId()).stream().map(r->ler(u,r)).toList();}
}
