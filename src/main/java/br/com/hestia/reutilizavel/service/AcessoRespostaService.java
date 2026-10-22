package br.com.hestia.reutilizavel.service;

import br.com.hestia.usuario.model.Usuario;
import br.com.hestia.reutilizavel.model.RespostaReutilizavel;
import br.com.hestia.reutilizavel.repository.RespostaReutilizavelRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

@Service
public class AcessoRespostaService {
    @org.springframework.beans.factory.annotation.Autowired(required=false)
    private br.com.hestia.auditoria.service.DecisaoRecuperacaoService decisoes;
    private final RespostaReutilizavelRepository repository;

    public AcessoRespostaService(RespostaReutilizavelRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void autorizarPerfis(Usuario usuario, Long id,
            java.util.Set<br.com.hestia.usuario.model.PerfilUsuario> perfis) {
        Long empresaId = validarEmpresa(usuario, null);
        if (usuario.getPerfil() != br.com.hestia.usuario.model.PerfilUsuario.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Somente ADMIN pode autorizar perfis");
        }
        if (perfis == null || perfis.stream().anyMatch(java.util.Objects::isNull)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Perfis inválidos");
        }
        RespostaReutilizavel resposta = repository.findById(id)
                .filter(r -> empresaId.equals(r.getEmpresaId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        resposta.setPerfisPermitidos(perfis);
        repository.save(resposta);
    }

    public Long validarEmpresa(Usuario usuario, Long empresaId) {
        if (usuario == null || usuario.getId() == null || !Boolean.TRUE.equals(usuario.getAtivo())
                || usuario.getEmpresa() == null || usuario.getEmpresa().getId() == null
                || usuario.getPerfil() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Contexto de acesso inválido");
        }
        Long autorizada = usuario.getEmpresa().getId();
        if (empresaId != null && !autorizada.equals(empresaId)) {
            if(decisoes!=null)decisoes.registrar(usuario,null,"ESCOPO_EMPRESA",false,"EMPRESA_NAO_AUTORIZADA");
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Empresa não autorizada");
        }
        return autorizada;
    }

    @Transactional(readOnly = true)
    public List<RespostaReutilizavel> recuperar(Usuario usuario, Long empresaId, Long id,
                                               String prompt, boolean exigirEmbedding, Pageable limite) {
        Long autorizada = validarEmpresa(usuario, empresaId);
        String filtro = prompt == null ? null : prompt.replace("!", "!!").replace("%", "!%")
                .replace("_", "!_");
        var resultado=repository.recuperarAutorizadas(autorizada, usuario.getPerfil(), id, filtro,
                exigirEmbedding, java.time.LocalDateTime.now(),limite);
        if(decisoes!=null)decisoes.registrar(usuario,id,"RECUPERACAO_TEXTUAL",id==null||!resultado.isEmpty(),resultado.isEmpty()?"SEM_CONTEUDO_AUTORIZADO":"EMPRESA_PERFIL_ELEGIBILIDADE_VALIDOS");
        return resultado;
    }

    @Transactional(readOnly = true)
    public RespostaReutilizavel buscarPorId(Usuario usuario, Long id) {
        return recuperar(usuario, null, id, null, false, Pageable.unpaged()).stream().findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Resposta não encontrada ou indisponível para seu acesso"));
    }
}
