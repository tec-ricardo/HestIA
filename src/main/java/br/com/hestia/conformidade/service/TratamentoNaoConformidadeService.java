package br.com.hestia.conformidade.service;

import br.com.hestia.autenticacao.contexto.EmpresaAtual;
import br.com.hestia.conformidade.dto.*;
import br.com.hestia.conformidade.model.*;
import br.com.hestia.conformidade.repository.*;
import br.com.hestia.politica.dto.PoliticaUsoVersaoDTO;
import br.com.hestia.registro.dto.RegistroUsoIAResponseDTO;
import br.com.hestia.usuario.model.*;
import br.com.hestia.usuario.repository.UsuarioRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.NoSuchElementException;
import java.util.Objects;

@Service
@Transactional
public class TratamentoNaoConformidadeService {
    private final TratamentoNaoConformidadeRepository tratamentos;
    private final AvaliacaoConformidadeRepository avaliacoes;
    private final UsuarioRepository usuarios;
    private final EmpresaAtual empresaAtual;

    public TratamentoNaoConformidadeService(TratamentoNaoConformidadeRepository tratamentos,
            AvaliacaoConformidadeRepository avaliacoes, UsuarioRepository usuarios, EmpresaAtual empresaAtual) {
        this.tratamentos = tratamentos;
        this.avaliacoes = avaliacoes;
        this.usuarios = usuarios;
        this.empresaAtual = empresaAtual;
    }

    public TratamentoDetalheDTO abrir(AberturaTratamentoDTO dto) {
        var autor = autorizar();
        // O bloqueio serializa aberturas concorrentes para a mesma ocorrência.
        var avaliacao = avaliacoes.buscarBloqueada(dto.avaliacaoId()).orElseThrow(this::ausente);
        var origem = avaliacao.getRegistroUso().getUsuario();
        validarEscopo(autor, origem.getEmpresa().getId(),
                origem.getDepartamento() == null ? null : origem.getDepartamento().getId());
        if (avaliacao.getStatus() != StatusConformidade.NAO_CONFORME)
            throw new IllegalArgumentException("Somente avaliações NAO_CONFORME podem originar tratamento.");
        if (tratamentos.existsByAvaliacao_Id(avaliacao.getId()))
            throw new IllegalArgumentException("Já existe tratamento para esta avaliação.");
        return detalhar(tratamentos.save(new TratamentoNaoConformidade(avaliacao, autor)));
    }

    public TratamentoDetalheDTO atualizar(Long id, AtualizacaoTratamentoDTO dto) {
        var autor = autorizar();
        var t = tratamentos.buscarBloqueado(id).orElseThrow(this::ausente);
        validarEscopo(autor, t.getEmpresa().getId(), t.getDepartamento().getId());
        if (!Objects.equals(dto.versaoRegistro(), t.getVersaoRegistro()))
            throw new IllegalArgumentException("Tratamento alterado por outra pessoa. Recarregue antes de salvar.");
        Usuario responsavel = null;
        if (dto.responsavelId() != null) {
            responsavel = usuarios.findById(dto.responsavelId()).orElseThrow(this::ausente);
            validarEscopo(autor, responsavel.getEmpresa().getId(),
                    responsavel.getDepartamento() == null ? null : responsavel.getDepartamento().getId());
            if (!Boolean.TRUE.equals(responsavel.getAtivo()))
                throw new IllegalArgumentException("Responsável deve estar ativo.");
        }
        t.atualizar(responsavel, dto.acaoCorretiva(), dto.status());
        tratamentos.flush();
        return detalhar(t);
    }

    @Transactional(readOnly = true)
    public TratamentoDetalheDTO buscar(Long id) {
        var autor = autorizar();
        var t = tratamentos.findById(id).orElseThrow(this::ausente);
        validarEscopo(autor, t.getEmpresa().getId(), t.getDepartamento().getId());
        return detalhar(t);
    }

    @Transactional(readOnly = true)
    public PaginaTratamentosDTO listar(StatusTratamento status, int pagina, int tamanho) {
        var autor = autorizar();
        if (pagina < 0 || tamanho < 1 || tamanho > 100)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Página deve ser não negativa e tamanho deve estar entre 1 e 100.");
        Specification<TratamentoNaoConformidade> filtro = (root, query, cb) -> {
            var condicao = cb.equal(root.get("empresa").get("id"), empresaAtual.obterId());
            if (autor.getPerfil() == PerfilUsuario.GESTOR)
                condicao = cb.and(condicao, cb.equal(root.get("departamento").get("id"),
                        autor.getDepartamento().getId()));
            if (status != null) condicao = cb.and(condicao, cb.equal(root.get("status"), status));
            return condicao;
        };
        return PaginaTratamentosDTO.from(tratamentos.findAll(filtro,
                PageRequest.of(pagina, tamanho, Sort.by(Sort.Direction.DESC, "criadoEm", "id")))
                .map(TratamentoResumoDTO::from));
    }

    private Usuario autorizar() {
        var u = usuarios.findById(empresaAtual.obterContexto().usuarioId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN));
        if (!Boolean.TRUE.equals(u.getAtivo()) || u.getEmpresa() == null
                || !Objects.equals(u.getEmpresa().getId(), empresaAtual.obterId())
                || (u.getPerfil() != PerfilUsuario.ADMIN && u.getPerfil() != PerfilUsuario.GESTOR))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Perfil não autorizado para tratar ocorrências.");
        if (u.getPerfil() == PerfilUsuario.GESTOR && (u.getDepartamento() == null
                || !Objects.equals(u.getDepartamento().getEmpresa().getId(), empresaAtual.obterId())))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Gestor sem escopo departamental válido.");
        return u;
    }

    private void validarEscopo(Usuario autor, Long empresaId, Long departamentoId) {
        if (!Objects.equals(empresaAtual.obterId(), empresaId)
                || (autor.getPerfil() == PerfilUsuario.GESTOR
                    && !Objects.equals(autor.getDepartamento().getId(), departamentoId)))
            throw ausente();
    }

    private TratamentoDetalheDTO detalhar(TratamentoNaoConformidade t) {
        var a = t.getAvaliacao();
        // Não substitui a revisão usada pela política vigente hoje.
        var politica = a.getPoliticaVersao() == null ? null : PoliticaUsoVersaoDTO.from(a.getPoliticaVersao());
        return new TratamentoDetalheDTO(TratamentoResumoDTO.from(t),
                RegistroUsoIAResponseDTO.from(a.getRegistroUso()),
                AvaliacaoConformidadeResponseDTO.from(a), politica);
    }

    private NoSuchElementException ausente() {
        return new NoSuchElementException("Tratamento ou avaliação não encontrado no escopo autorizado.");
    }
}
