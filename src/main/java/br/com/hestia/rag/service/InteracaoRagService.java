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
    public record Pergunta(String chave,String pergunta,Modo modo,Long respostaId,Long conversaId){
        public Pergunta(String chave,String pergunta,Modo modo,Long respostaId){this(chave,pergunta,modo,respostaId,null);}
    }
    public record Conversa(Long id,String titulo,LocalDateTime criadaEm){}
    public record Fonte(Long id,String titulo,String trecho,Integer versao,String conteudoSha256){}
    public record Resultado(Long id,String pergunta,LocalDateTime criadaEm,String estado,String resposta,List<Fonte> fontes,Double similaridade,String confianca,String natureza,String aviso){}
    private final br.com.hestia.shared.RepositorioIntegracao db;private final BuscaVetorialService busca;private final AcessoRespostaService acesso;
    private final GeracaoRagService geracao;private final RespostaReutilizavelService respostas;private final RegistroUsoIAService registros;private final Long ferramentaId;
    public InteracaoRagService(br.com.hestia.shared.RepositorioIntegracao db,BuscaVetorialService busca,AcessoRespostaService acesso,GeracaoRagService geracao,RespostaReutilizavelService respostas,RegistroUsoIAService registros,@Value("${hestia.rag.ferramenta-id:0}")Long ferramentaId){this.db=db;this.busca=busca;this.acesso=acesso;this.geracao=geracao;this.respostas=respostas;this.registros=registros;this.ferramentaId=ferramentaId;}
    public Resultado perguntar(Usuario u,Pergunta p){
        Long empresa=acesso.validarEmpresa(u,null);exigir(p!=null&&p.chave()!=null&&!p.chave().isBlank()&&p.chave().length()<=100&&p.pergunta()!=null&&!p.pergunta().isBlank()&&p.pergunta().length()<=4000&&p.modo()!=null,HttpStatus.BAD_REQUEST,"Pergunta, modo e chave obrigatórios");db.acesso().serializar(u.getId());
        var anteriores=db.jdbc().queryForList("SELECT * FROM interacoes_rag WHERE empresa_id=? AND usuario_id=? AND chave=?",empresa,u.getId(),p.chave());
        if(!anteriores.isEmpty()){var r=anteriores.getFirst();exigir(p.pergunta().equals(r.get("pergunta"))&&p.modo().name().equals(r.get("modo"))&&(p.conversaId()==null||p.conversaId().equals(id(r,"conversa_id"))),HttpStatus.CONFLICT,"Chave já utilizada para outra interação");return ler(u,r);}
        Long conversa=p.conversaId();
        if(conversa==null)conversa=criarConversa(u,p.pergunta()).id();
        else validarConversa(u,empresa,conversa);
        List<RespostaReutilizavel> candidatas;
        if(p.modo()==Modo.REUTILIZAR){exigir(p.respostaId()!=null,HttpStatus.BAD_REQUEST,"Resposta para reutilização obrigatória");candidatas=List.of(acesso.buscarPorId(u,p.respostaId()));}
        else {
            // Perguntas já conhecidas não precisam consumir embeddings. A recuperação
            // textual aplica os mesmos filtros de empresa, perfil, revisão, validade
            // e sensibilidade usados pela recuperação vetorial.
            candidatas=respostas.buscarRespostasAnteriores(u,empresa,p.pergunta());
            if(candidatas.isEmpty()){
                try{candidatas=busca.buscarSemelhantes(empresa,p.pergunta(),5);}
                catch(org.springframework.web.server.ResponseStatusException e){throw e;}
                catch(RuntimeException e){org.slf4j.LoggerFactory.getLogger(InteracaoRagService.class).error("Falha real na recuperação vetorial do RAG",e);throw erro(HttpStatus.SERVICE_UNAVAILABLE,"Recuperação vetorial indisponível; consulte o log Java");}
            }
        }
        // Revalidação após a busca: somente estes trechos entram no prompt de geração.
        List<Fonte> fontes=new ArrayList<>();int caracteres=0;
        for(var candidata:candidatas){var r=acesso.buscarPorId(u,candidata.getId());String trecho=r.getRespostaGerada();if(trecho.length()>6000)trecho=trecho.substring(0,6000);if(caracteres+trecho.length()>18000)break;caracteres+=trecho.length();var versoes=db.jdbc().queryForList("SELECT numero_versao,conteudo_sha256 FROM resposta_versoes WHERE resposta_id=? ORDER BY numero_versao DESC",r.getId());fontes.add(new Fonte(r.getId(),r.getPromptOriginal(),trecho,versoes.isEmpty()?null:((Number)versoes.getFirst().get("numero_versao")).intValue(),versoes.isEmpty()?hash(r.getPromptOriginal()+"\n"+r.getRespostaGerada()):(String)versoes.getFirst().get("conteudo_sha256")));}
        String estado=fontes.isEmpty()?"SEM_CONTEUDO_AUTORIZADO":"RECUPERADA";String texto=fontes.isEmpty()?null:fontes.getFirst().trecho();String modelo=null;Long entrada=null,saida=null,registro=null;
        if(!fontes.isEmpty()&&p.modo()==Modo.GERAR_NOVA){var resposta=geracao.gerar(p.pergunta(),fontes.stream().map(f->"[Fonte "+f.id()+"] "+f.trecho()).toList());texto=resposta.texto();modelo=resposta.modelo();entrada=resposta.tokensEntrada();saida=resposta.tokensSaida();estado="GERADA_COM_FONTES";
            if(ferramentaId>0&&entrada!=null&&saida!=null&&entrada>=0&&saida>=0&&entrada<=Integer.MAX_VALUE&&saida<=Integer.MAX_VALUE&&entrada+saida<=Integer.MAX_VALUE&&db.jdbc().queryForObject("SELECT count(*) FROM ferramentas_ia WHERE id=? AND empresa_id=? AND status='APROVADA'",Long.class,ferramentaId,empresa)==1)registro=registros.criarCapturado(new RegistroUsoIACriacaoDTO(u.getId(),ferramentaId,modelo,LocalDateTime.now(),FinalidadeUso.GERACAO_CONTEUDO,entrada.intValue(),saida.intValue(),null,StatusExecucao.SUCESSO,null),"RAG:"+u.getId()+":"+p.chave()).id();
        }
        // Respostas novas, fundamentadas em fontes autorizadas, entram na curadoria.
        // Não publicar automaticamente: cadastrar() atribui EM_REVISAO.
        if ("GERADA_COM_FONTES".equals(estado) && texto != null && !texto.isBlank()) {
            var pendente = new br.com.hestia.reutilizavel.dto.RespostaReutilizavelDTO();
            pendente.setEmpresaId(empresa);
            pendente.setPromptOriginal(p.pergunta());
            pendente.setRespostaGerada(texto);
            pendente.setCategoria("RAG - Aguardando curadoria");
            pendente.setModeloIA(modelo);
            if (entrada != null && entrada >= 0 && entrada <= Integer.MAX_VALUE) pendente.setTokensEntrada(entrada.intValue());
            if (saida != null && saida >= 0 && saida <= Integer.MAX_VALUE) pendente.setTokensSaida(saida.intValue());
            pendente.setPossuiDadosSensiveis(true); // conservador: revisão explícita antes de aprovar
            pendente.setReutilizavel(false);
            respostas.cadastrar(pendente, u);
        }
        // Entregar diretamente uma resposta aprovada também é uma reutilização.
        // O usuário não precisa confirmar a mesma ação em um segundo botão.
        if(!fontes.isEmpty()&&(p.modo()==Modo.CONSULTAR||p.modo()==Modo.REUTILIZAR)){
            respostas.registrarReutilizacao(fontes.getFirst().id(),u.getId());
            estado="REUTILIZADA";
        }
        Long nova=db.inserir("INSERT INTO interacoes_rag(empresa_id,usuario_id,chave,pergunta,modo,estado,resposta,fontes,modelo,tokens_entrada,tokens_saida,registro_uso_id,criada_em,conversa_id) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?)",empresa,u.getId(),p.chave(),p.pergunta(),p.modo().name(),estado,texto,db.escrever(fontes),modelo,entrada,saida,registro,LocalDateTime.now(),conversa);
        return ler(u,db.jdbc().queryForList("SELECT * FROM interacoes_rag WHERE id=? AND empresa_id=? AND usuario_id=?",nova,empresa,u.getId()).getFirst());
    }
    Resultado ler(Usuario u,Map<String,Object> row){
        List<Fonte> fontes;try{fontes=db.json().readValue((String)row.get("fontes"),new com.fasterxml.jackson.core.type.TypeReference<List<Fonte>>(){});}catch(Exception e){throw new IllegalStateException(e);}
        for(var f:fontes){try{var atual=acesso.buscarPorId(u,f.id());String atualHash=hash(atual.getPromptOriginal()+"\n"+atual.getRespostaGerada());var versoes=db.jdbc().queryForList("SELECT conteudo_sha256 FROM resposta_versoes WHERE resposta_id=? ORDER BY numero_versao DESC",f.id());if(!versoes.isEmpty())atualHash=(String)versoes.getFirst().get("conteudo_sha256");if(!atualHash.equals(f.conteudoSha256()))throw erro(HttpStatus.NOT_FOUND,"Fonte alterada");}catch(org.springframework.web.server.ResponseStatusException e){return new Resultado(id(row,"id"),(String)row.get("pergunta"),((java.sql.Timestamp)row.get("criada_em")).toLocalDateTime(),"FONTES_INDISPONIVEIS",null,List.of(),null,"NAO_CLASSIFICADA","PLATAFORMA","Fonte alterada, retirada, expirada ou sem permissão atual");}}
        return new Resultado(id(row,"id"),(String)row.get("pergunta"),((java.sql.Timestamp)row.get("criada_em")).toLocalDateTime(),(String)row.get("estado"),(String)row.get("resposta"),fontes,null,"NAO_CLASSIFICADA","PLATAFORMA","Histórico restrito ao autor e revalidado no acesso");
    }
    public List<Resultado> historico(Usuario u){Long empresa=acesso.validarEmpresa(u,null);return db.jdbc().queryForList("SELECT * FROM interacoes_rag WHERE empresa_id=? AND usuario_id=? ORDER BY id DESC LIMIT 100",empresa,u.getId()).stream().map(r->ler(u,r)).toList();}
    public Conversa criarConversa(Usuario u,String titulo){
        Long empresa=acesso.validarEmpresa(u,null);
        exigir(titulo!=null&&!titulo.isBlank(),HttpStatus.BAD_REQUEST,"Título obrigatório");
        String nome=titulo.strip();if(nome.length()>120)nome=nome.substring(0,120);
        var agora=LocalDateTime.now();
        Long id=db.inserir("INSERT INTO conversas_rag(empresa_id,usuario_id,titulo,criada_em) VALUES(?,?,?,?)",empresa,u.getId(),nome,agora);
        return new Conversa(id,nome,agora);
    }
    public List<Conversa> conversas(Usuario u){
        Long empresa=acesso.validarEmpresa(u,null);
        return db.jdbc().queryForList("SELECT * FROM conversas_rag WHERE empresa_id=? AND usuario_id=? ORDER BY id DESC LIMIT 100",empresa,u.getId()).stream()
                .map(r->new Conversa(id(r,"id"),(String)r.get("titulo"),((java.sql.Timestamp)r.get("criada_em")).toLocalDateTime())).toList();
    }
    public List<Resultado> historicoConversa(Usuario u,Long conversa){
        Long empresa=acesso.validarEmpresa(u,null);validarConversa(u,empresa,conversa);
        var linhas=db.jdbc().queryForList("SELECT * FROM interacoes_rag WHERE empresa_id=? AND usuario_id=? AND conversa_id=? ORDER BY id DESC LIMIT 100",empresa,u.getId(),conversa);
        Collections.reverse(linhas);return linhas.stream().map(r->ler(u,r)).toList();
    }
    private void validarConversa(Usuario u,Long empresa,Long conversa){
        exigir(db.jdbc().queryForObject("SELECT count(*) FROM conversas_rag WHERE id=? AND empresa_id=? AND usuario_id=?",Long.class,conversa,empresa,u.getId())==1,HttpStatus.NOT_FOUND,"Conversa não encontrada");
    }
}