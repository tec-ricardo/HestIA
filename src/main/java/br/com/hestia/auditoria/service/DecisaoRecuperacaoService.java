package br.com.hestia.auditoria.service;
import br.com.hestia.auditoria.model.DecisaoRecuperacao;
import br.com.hestia.auditoria.repository.DecisaoRecuperacaoRepository;
import br.com.hestia.usuario.model.Usuario;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
@Service
public class DecisaoRecuperacaoService {
    private final DecisaoRecuperacaoRepository repository;
    public DecisaoRecuperacaoService(DecisaoRecuperacaoRepository repository){this.repository=repository;}
    @Transactional(propagation=Propagation.REQUIRES_NEW)
    public void registrar(Usuario u,Long resposta,String recurso,boolean permitida,String motivo){
        if(u!=null&&u.getId()!=null&&u.getEmpresa()!=null)repository.save(new DecisaoRecuperacao(u.getEmpresa().getId(),u.getId(),resposta,recurso,permitida?"PERMITIDA":"BLOQUEADA",motivo));
    }
}
