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
        RespostaReutilizavel resposta = repository.buscarBloqueada(id)
                .filter(r -> empresaId.equals(r.getEmpresaId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        resposta.setPerfisPermitidos(perfis);
        repository.save(resposta);
    }

    @Transactional
    public RespostaReutilizavel buscarBloqueadaParaReutilizar(Usuario usuario, Long id) {
        Long empresaId = validarEmpresa(usuario, null);
        return repository.buscarBloqueada(id)
                .filter(r -> empresaId.equals(r.getEmpresaId()))
                .filter(r -> r.getPerfisPermitidos().contains(usuario.getPerfil()))
                .filter(r -> Boolean.TRUE.equals(r.getReutilizavel()))
                .filter(r -> Boolean.FALSE.equals(r.getPossuiDadosSensiveis()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Resposta não encontrada ou indisponível para seu acesso"));
    }

    public Long validarEmpresa(Usuario usuario, Long empresaId) {
        if (usuario == null || usuario.getId() == null || !Boolean.TRUE.equals(usuario.getAtivo())
                || usuario.getEmpresa() == null || usuario.getEmpresa().getId() == null
                || usuario.getPerfil() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Contexto de acesso inválido");
        }
        Long autorizada = usuario.getEmpresa().getId();
        if (empresaId != null && !autorizada.equals(empresaId)) {
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
        return repository.recuperarAutorizadas(autorizada, usuario.getPerfil(), id, filtro,
                exigirEmbedding, limite);
    }

    @Transactional(readOnly = true)
    public RespostaReutilizavel buscarPorId(Usuario usuario, Long id) {
        return recuperar(usuario, null, id, null, false, Pageable.unpaged()).stream().findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Resposta não encontrada ou indisponível para seu acesso"));
    }
}
