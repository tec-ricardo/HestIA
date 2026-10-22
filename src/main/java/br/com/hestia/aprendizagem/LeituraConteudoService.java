package br.com.hestia.aprendizagem;
import br.com.hestia.microtreinamento.service.ConclusaoMicrotreinamentoService;
import br.com.hestia.usuario.model.Usuario;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import static br.com.hestia.aprendizagem.AcessoCapacitacao.*;

@Service @Transactional
public class LeituraConteudoService {
    private final AvaliacaoAprendizagemService aprendizagem;
    private final ConclusaoMicrotreinamentoService conclusoes;
    public LeituraConteudoService(AvaliacaoAprendizagemService aprendizagem,ConclusaoMicrotreinamentoService conclusoes){this.aprendizagem=aprendizagem;this.conclusoes=conclusoes;}
    public Long concluir(Usuario autor,Long treinamento,String chave){
        exigir(chave!=null&&!chave.isBlank()&&chave.length()<=100,HttpStatus.BAD_REQUEST,"Chave de confirmação obrigatória");
        var a=aprendizagem;var u=a.acesso().usuario(autor,autor.getId());a.acesso().serializar(u.getId());var conteudo=a.conteudo(treinamento);
        var repetidos=a.jdbc().queryForList("SELECT id,treinamento_id,conteudo_sha256 FROM aprendizagem_leituras WHERE empresa_id=? AND usuario_id=? AND chave_evento=?",a.acesso().empresa(u),u.getId(),chave);
        if(!repetidos.isEmpty()){var r=repetidos.getFirst();exigir(treinamento.equals(AvaliacaoAprendizagemService.id(r,"treinamento_id"))&&hash(conteudo.getConteudo()).equals(r.get("conteudo_sha256")),HttpStatus.CONFLICT,"Chave usada para outro conteúdo");return AvaliacaoAprendizagemService.id(r,"id");}
        var c=conclusoes.registrarConclusao(u.getId(),treinamento);
        // Nova evidência explícita de leitura; a conclusão histórica não é reescrita.
        Long id=a.inserir("INSERT INTO aprendizagem_leituras(empresa_id,usuario_id,treinamento_id,conteudo_sha256,conclusao_id,concluida_em,chave_evento) VALUES(?,?,?,?,?,?,?)",a.acesso().empresa(u),u.getId(),treinamento,hash(conteudo.getConteudo()),c.getId(),java.time.LocalDateTime.now(),chave);
        a.eventos().publishEvent(new ContratoAvaliacao.Atualizada(u.getId(),a.acesso().empresa(u)));return id;
    }
}
