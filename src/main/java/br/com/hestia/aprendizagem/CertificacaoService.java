package br.com.hestia.aprendizagem;
import br.com.hestia.usuario.model.Usuario;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.http.HttpStatus;
import java.util.*;
import static br.com.hestia.aprendizagem.AcessoCapacitacao.*;
import static br.com.hestia.aprendizagem.AvaliacaoAprendizagemService.id;

@Service @Transactional
public class CertificacaoService {
    private final AvaliacaoAprendizagemService a;
    public CertificacaoService(AvaliacaoAprendizagemService a){this.a=a;}
    public record Publicacao(String codigo,String nome,String nivel,int versao,List<Long> avaliacoesObrigatorias){}
    public record Situacao(Long definicaoId,String codigo,String nome,String nivel,int versao,boolean concedida,int requisitosAtendidos,int totalRequisitos,List<Long> avaliacoesPendentes){}
    public long publicar(Usuario autor,Publicacao d){
        a.acesso().administrador(autor);exigir(d!=null&&AvaliacaoAprendizagemService.texto(d.codigo(),100)&&AvaliacaoAprendizagemService.texto(d.nome(),255)&&AvaliacaoAprendizagemService.texto(d.nivel(),100)&&d.versao()>0&&d.avaliacoesObrigatorias()!=null&&!d.avaliacoesObrigatorias().isEmpty()&&d.avaliacoesObrigatorias().size()<=100&&new HashSet<>(d.avaliacoesObrigatorias()).size()==d.avaliacoesObrigatorias().size(),HttpStatus.BAD_REQUEST,"Requisitos da certificação incompletos");
        Long empresa=a.acesso().empresa(autor);a.acesso().serializar(autor.getId());List<Map<String,Object>> requisitos=new ArrayList<>();Set<Long> treinamentos=new HashSet<>();
        for(Long avaliacao:d.avaliacoesObrigatorias()){var r=a.catalogo(empresa,avaliacao);exigir(treinamentos.add(id(r,"treinamento_id")),HttpStatus.BAD_REQUEST,"Informe uma versão por microtreinamento");requisitos.add(Map.of("catalogoId",avaliacao,"treinamentoId",id(r,"treinamento_id"),"conteudoSha256",r.get("conteudo_sha256"),"versaoAvaliacao",r.get("versao")));}
        exigir(a.jdbc().queryForObject("SELECT count(*) FROM certificacao_definicoes WHERE empresa_id=? AND codigo=? AND versao>=?",Long.class,empresa,d.codigo(),d.versao())==0,HttpStatus.CONFLICT,"Versão já publicada ou anterior à vigente");
        return a.inserir("INSERT INTO certificacao_definicoes(empresa_id,codigo,nome,nivel,versao,requisitos,publicado_por,publicada_em) VALUES(?,?,?,?,?,?,?,?)",empresa,d.codigo(),d.nome(),d.nivel(),d.versao(),a.escrever(requisitos),autor.getId(),java.time.LocalDateTime.now());
    }
    @TransactionalEventListener(phase=TransactionPhase.BEFORE_COMMIT)
    public void aprendizagemAtualizada(ContratoAvaliacao.Atualizada e){verificarInterno(e.usuarioId(),e.empresaId());}
    public List<Situacao> verificar(Usuario autor,Long usuarioId){var u=a.acesso().usuario(autor,usuarioId);a.acesso().serializar(u.getId());return verificarInterno(u.getId(),a.acesso().empresa(u));}
    List<Situacao> verificarInterno(Long usuario,Long empresa){
        a.acesso().serializar(usuario);
        if(a.jdbc().queryForObject("SELECT count(*) FROM usuarios WHERE id=? AND empresa_id=? AND ativo=true",Long.class,usuario,empresa)==0)return List.of();
        List<Situacao> saida=new ArrayList<>();
        for(var def:a.jdbc().queryForList("SELECT * FROM certificacao_definicoes d WHERE empresa_id=? AND versao=(SELECT max(n.versao) FROM certificacao_definicoes n WHERE n.empresa_id=d.empresa_id AND n.codigo=d.codigo) ORDER BY id",empresa)){
            Long definicao=id(def,"id");List<Long> pendentes=new ArrayList<>();List<Map<String,Object>> evidencias=new ArrayList<>();int totalRequisitos=0;
            try {
                var requisitos=a.json().readTree((String)def.get("requisitos"));
                totalRequisitos=requisitos.size();
                for(var req:requisitos){long catalogo=req.get("catalogoId").asLong();
                    var provas=a.jdbc().queryForList("SELECT t.id AS tentativa_id,t.nota,t.finalizada_em,c.catalogo_id,a.versao,a.conteudo_sha256,l.id AS leitura_id,l.conclusao_id FROM aprendizagem_tentativas t JOIN aprendizagem_ciclos c ON c.id=t.ciclo_id JOIN aprendizagem_catalogo a ON a.id=c.catalogo_id JOIN aprendizagem_leituras l ON l.empresa_id=c.empresa_id AND l.usuario_id=c.usuario_id AND l.treinamento_id=a.treinamento_id AND l.conteudo_sha256=a.conteudo_sha256 WHERE c.empresa_id=? AND c.usuario_id=? AND c.catalogo_id=? AND t.status='APROVADA' AND t.finalizada_em IS NOT NULL ORDER BY t.id,l.id",empresa,usuario,catalogo);
                    if(provas.isEmpty())pendentes.add(catalogo);else evidencias.add(provas.getFirst());
                }
            }catch(com.fasterxml.jackson.core.JsonProcessingException e){throw new IllegalStateException(e);}
            boolean concedida=a.jdbc().queryForObject("SELECT count(*) FROM certificacao_concessoes WHERE empresa_id=? AND usuario_id=? AND definicao_id=?",Long.class,empresa,usuario,definicao)>0;
            if(!concedida&&pendentes.isEmpty()){
                a.inserir("INSERT INTO certificacao_concessoes(empresa_id,usuario_id,definicao_id,codigo,nome,nivel,versao,concedida_em,evidencias) VALUES(?,?,?,?,?,?,?,?,?)",empresa,usuario,definicao,def.get("codigo"),def.get("nome"),def.get("nivel"),def.get("versao"),java.time.LocalDateTime.now(),a.escrever(evidencias));concedida=true;
            }
            saida.add(new Situacao(definicao,(String)def.get("codigo"),(String)def.get("nome"),(String)def.get("nivel"),((Number)def.get("versao")).intValue(),concedida,totalRequisitos-pendentes.size(),totalRequisitos,List.copyOf(pendentes)));
        }return saida;
    }
    public List<Map<String,Object>> historico(Usuario autor,Long usuarioId){var u=a.acesso().usuario(autor,usuarioId);return a.jdbc().queryForList("SELECT * FROM certificacao_concessoes WHERE empresa_id=? AND usuario_id=? ORDER BY id",a.acesso().empresa(u),u.getId());}
}
