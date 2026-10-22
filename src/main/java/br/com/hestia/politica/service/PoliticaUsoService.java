package br.com.hestia.politica.service;

import br.com.hestia.auditoria.service.AuditoriaService;
import br.com.hestia.autenticacao.contexto.EmpresaAtual;
import br.com.hestia.empresa.repository.EmpresaRepository;
import br.com.hestia.politica.dto.*;
import br.com.hestia.politica.model.*;
import br.com.hestia.politica.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.NoSuchElementException;

@Service
@Transactional
public class PoliticaUsoService {
    private final PoliticaUsoRepository politicas;
    private final EmpresaRepository empresas;
    private final AuditoriaService auditoria;
    private final PoliticaUsoVersaoRepository versoes;
    private final EmpresaAtual empresaAtual;

    public PoliticaUsoService(PoliticaUsoRepository politicas, EmpresaRepository empresas,
            AuditoriaService auditoria, PoliticaUsoVersaoRepository versoes, EmpresaAtual empresaAtual) {
        this.politicas = politicas;
        this.empresas = empresas;
        this.auditoria = auditoria;
        this.versoes = versoes;
        this.empresaAtual = empresaAtual;
    }

    public PoliticaUso criar(PoliticaUsoDTO dto) {
        empresaAtual.validar(dto.getEmpresaId());
        var empresa = empresas.findById(dto.getEmpresaId())
                .orElseThrow(() -> new NoSuchElementException("Empresa não encontrada."));
        var p = new PoliticaUso();
        p.setEmpresa(empresa);
        aplicar(p, dto);
        politicas.save(p);
        registrarVersao(p, "CRIACAO");
        registrar("POST", p.getId());
        return p;
    }

    @Transactional(readOnly = true)
    public List<PoliticaUso> listarTodas() {
        return listarPorEmpresa(empresaAtual.obterId());
    }

    @Transactional(readOnly = true)
    public List<PoliticaUso> listarPorEmpresa(Long empresaId) {
        empresaAtual.validar(empresaId);
        return politicas.findByEmpresaId(empresaId).stream().filter(p -> !p.isExcluida()).toList();
    }

    @Transactional(readOnly = true)
    public PoliticaUso buscarPorId(Long id) {
        var p = obter(id, false);
        if (p.isExcluida()) throw new NoSuchElementException("Política de uso não encontrada.");
        return p;
    }

    public PoliticaUso atualizar(Long id, PoliticaUsoDTO dto) {
        var p = obter(id, true);
        if (p.isExcluida()) throw new NoSuchElementException("Política de uso não encontrada.");
        empresaAtual.validar(dto.getEmpresaId());
        if (!p.getEmpresa().getId().equals(dto.getEmpresaId()))
            throw new IllegalArgumentException("Não é permitido transferir uma política entre empresas.");
        aplicar(p, dto);
        p.avancarRevisao();
        politicas.save(p);
        registrarVersao(p, "ATUALIZACAO");
        registrar("PUT", p.getId());
        return p;
    }

    public void excluir(Long id) {
        var p = obter(id, true);
        if (!p.isExcluida()) {
            p.excluirLogicamente();
            p.avancarRevisao();
            politicas.save(p);
            registrarVersao(p, "EXCLUSAO");
            registrar("DELETE", id);
        }
    }

    @Transactional(readOnly = true)
    public List<PoliticaUsoVersaoDTO> listarVersoes(Long id) {
        obter(id, false);
        return versoes.findByPoliticaUsoIdOrderByRevisaoDesc(id).stream()
                .map(PoliticaUsoVersaoDTO::from).toList();
    }

    @Transactional(readOnly = true)
    public PoliticaUsoVersaoDTO buscarVersao(Long id, int revisao) {
        obter(id, false);
        return PoliticaUsoVersaoDTO.from(versoes.findByPoliticaUsoIdAndRevisao(id, revisao)
                .orElseThrow(() -> new NoSuchElementException("Revisão da política não encontrada.")));
    }

    private PoliticaUso obter(Long id, boolean bloquear) {
        var p = (bloquear ? politicas.buscarBloqueada(id) : politicas.findById(id))
                .orElseThrow(() -> new NoSuchElementException("Política de uso não encontrada."));
        empresaAtual.validar(p.getEmpresa().getId());
        return p;
    }

    private void aplicar(PoliticaUso p, PoliticaUsoDTO dto) {
        p.setTitulo(dto.getTitulo());
        p.setDescricao(dto.getDescricao());
        p.setConteudo(dto.getConteudo());
        p.setVersao(dto.getVersao());
        if (dto.getAtiva() != null) p.setAtiva(dto.getAtiva());
    }

    private void registrarVersao(PoliticaUso p, String origem) {
        versoes.save(new PoliticaUsoVersao(p, empresaAtual.obterContexto().usuarioId(), origem));
    }

    private void registrar(String metodo, Long id) {
        auditoria.registrarOperacao(metodo, "/politicas/" + id, 200);
    }
}
