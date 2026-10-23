
package br.com.hestia.reutilizavel.service;

import br.com.hestia.autenticacao.contexto.EmpresaAtual;
import br.com.hestia.gamificacao.dto.RegistroAcaoDTO;
import br.com.hestia.gamificacao.model.AcaoExperiencia;
import br.com.hestia.gamificacao.service.GamificacaoService;
import br.com.hestia.registro.repository.RegistroUsoIARepository;
import br.com.hestia.reutilizavel.dto.EconomiaReutilizacaoDTO;
import br.com.hestia.reutilizavel.dto.IndicadoresQualidadeDTO;
import br.com.hestia.reutilizavel.dto.RespostaReutilizavelDTO;
import br.com.hestia.reutilizavel.dto.SustentabilidadeDTO;
import br.com.hestia.reutilizavel.model.RespostaReutilizavel;
import br.com.hestia.reutilizavel.repository.RespostaReutilizavelRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import br.com.hestia.usuario.model.Usuario;
import org.springframework.data.domain.PageRequest;
import java.util.List;
import java.util.NoSuchElementException;

@Service
public class RespostaReutilizavelService {


    private final RespostaReutilizavelRepository repository;
    private final AcessoRespostaService acesso;
    private final GamificacaoService gamificacaoService;
    private final RegistroUsoIARepository registroUsoIARepository;
    private final EmpresaAtual empresaAtual;

    @Autowired
    public RespostaReutilizavelService(
            RespostaReutilizavelRepository repository,
            GamificacaoService gamificacaoService,
            RegistroUsoIARepository registroUsoIARepository,
            EmpresaAtual empresaAtual,
            AcessoRespostaService acesso
    ) {
        this.repository = repository;
        this.gamificacaoService = gamificacaoService;
        this.registroUsoIARepository = registroUsoIARepository;
        this.empresaAtual = empresaAtual;
        this.acesso = acesso;
    }

    public RespostaReutilizavelService(
            RespostaReutilizavelRepository repository,
            GamificacaoService gamificacaoService,
            RegistroUsoIARepository registroUsoIARepository,
            AcessoRespostaService acesso
    ) {
        this(repository, gamificacaoService, registroUsoIARepository, null, acesso);
    }


    public RespostaReutilizavel cadastrar(
            RespostaReutilizavelDTO dto, Usuario usuario
    ) {

        if (empresaAtual != null) {
            empresaAtual.validar(dto.getEmpresaId());
        }

        RespostaReutilizavel resposta =
                new RespostaReutilizavel();
        resposta.setStatusQualidade(br.com.hestia.reutilizavel.model.StatusQualidadeResposta.EM_REVISAO);

        Long empresaId = acesso.validarEmpresa(usuario, dto.getEmpresaId());
        resposta.setPerfisPermitidos(java.util.Set.of(usuario.getPerfil()));
        resposta.setUsuarioId(usuario.getId());
        resposta.setEmpresaId(empresaId);
        resposta.setPromptOriginal(dto.getPromptOriginal());
        resposta.setRespostaGerada(dto.getRespostaGerada());
        resposta.setEmbedding(dto.getEmbedding());
        resposta.setCategoria(dto.getCategoria());
        resposta.setModeloIA(dto.getModeloIA());
        resposta.setTokensEntrada(dto.getTokensEntrada());
        resposta.setTokensSaida(dto.getTokensSaida());
        resposta.setCustoEstimado(dto.getCustoEstimado());

        if (dto.getPossuiDadosSensiveis() != null) {
            resposta.setPossuiDadosSensiveis(
                    dto.getPossuiDadosSensiveis()
            );
        }

        if (dto.getReutilizavel() != null) {
            resposta.setReutilizavel(
                    dto.getReutilizavel()
            );
        }

        return repository.save(resposta);
    }

    public List<RespostaReutilizavel> listarTodas() {
        return empresaAtual == null
                ? repository.findAll()
                : repository.findByEmpresaId(empresaAtual.obterId());
    }

    public List<RespostaReutilizavel> listarPorEmpresa(
            Long empresaId
    ) {
        validarEmpresa(empresaId);
        return repository.findByEmpresaId(empresaId);
    }

    public List<RespostaReutilizavel>
    listarReutilizaveisPorEmpresa(
            Long empresaId
    ) {
        validarEmpresa(empresaId);
        return repository
                .findByEmpresaIdAndReutilizavelTrue(
                        empresaId
                );
    }

    @Transactional(readOnly = true)
    public List<RespostaReutilizavel> buscarRespostasAnteriores(
            Usuario usuario, Long empresaId, String prompt) {
        validarEmpresa(empresaId);
        if (prompt == null || prompt.isBlank()) {
            throw new IllegalArgumentException("Informe o prompt para recuperar respostas anteriores.");
        }
        return acesso.recuperar(usuario, empresaId, null,
                prompt.trim(), false, PageRequest.of(0, 10));
    }

    @Transactional(readOnly = true)
    public List<RespostaReutilizavel> recuperarPerguntasSemelhantes(Usuario usuario, Long empresaId) {
        validarEmpresa(empresaId);
        return acesso.recuperar(usuario, empresaId, null,
                null, true, org.springframework.data.domain.Pageable.unpaged());
    }

    public RespostaReutilizavel buscarPorId(Long id) {

        RespostaReutilizavel resposta = repository.findById(id)
                .orElseThrow(() ->
                        new NoSuchElementException(
                                "Resposta reutilizável não encontrada."
                        )
                );
        validarEmpresa(resposta.getEmpresaId());
        return resposta;
    }

    @Transactional
    public RespostaReutilizavel registrarReutilizacao(
            Long id, Long usuarioReutilizadorId
    ) {

        RespostaReutilizavel resposta =
                buscarPorId(id);

        if (!resposta.isElegivel()) {

            throw new IllegalStateException(
                    "A resposta selecionada não está disponível para reutilização."
            );
        }

        resposta.registrarReutilizacao();

        RespostaReutilizavel salva =
                repository.save(resposta);

        if (usuarioReutilizadorId != null) {

            gamificacaoService.registrarAcao(
                    new RegistroAcaoDTO(
                            usuarioReutilizadorId,
                            AcaoExperiencia.REUTILIZACAO_RESPOSTA,
                            "REUTILIZACAO:"
                                    + salva.getId()
                                    + ":"
                                    + salva.getQuantidadeReutilizacoes()
                    )
            );
        }

        return salva;
    }

    public EconomiaReutilizacaoDTO consultarEconomia(
            Long empresaId
    ) {

        validarEmpresa(empresaId);
        List<RespostaReutilizavel> respostas =
                repository.findByEmpresaId(empresaId);

        long chamadasEvitadas =
                respostas.stream()
                        .mapToLong(
                                RespostaReutilizavel::getChamadasEvitadas
                        )
                        .sum();

        long tokensEconomizados =
                respostas.stream()
                        .mapToLong(
                                RespostaReutilizavel::getTokensEconomizados
                        )
                        .sum();

        BigDecimal custoEstimadoEvitado =
                respostas.stream()
                        .map(
                                RespostaReutilizavel::getCustoEstimadoEvitado
                        )
                        .reduce(
                                BigDecimal.ZERO,
                                BigDecimal::add
                        );

        long chamadasRealizadas =
                registroUsoIARepository
                        .countByUsuarioEmpresaId(
                                empresaId
                        );

        long totalInteracoes =
                chamadasRealizadas
                        + chamadasEvitadas;

        double percentualReutilizacao =
                totalInteracoes == 0
                        ? 0
                        : chamadasEvitadas
                        * 100.0
                        / totalInteracoes;

        return new EconomiaReutilizacaoDTO(
                empresaId,
                chamadasEvitadas,
                tokensEconomizados,
                custoEstimadoEvitado,
                percentualReutilizacao
        );
    }

    @Transactional(readOnly = true)
    public IndicadoresQualidadeDTO consultarQualidade(Long empresaId) {
              validarEmpresa(empresaId);

        if (empresaId == null || empresaId <= 0) {
            throw new IllegalArgumentException(
                    "Informe uma empresa válida."
            );
        }

        List<RespostaReutilizavel> respostas =
                repository.findByEmpresaId(empresaId);

        long totalRespostas = respostas.size();

        long respostasDisponiveis = respostas.stream()
                .filter(r -> Boolean.TRUE.equals(r.getReutilizavel()))
                .count();

        long respostasSeguras = respostas.stream()
                .filter(r -> Boolean.TRUE.equals(r.getReutilizavel())
                        && Boolean.FALSE.equals(r.getPossuiDadosSensiveis()))
                .count();

        long respostasReutilizadas = respostas.stream()
                .filter(r -> r.getQuantidadeReutilizacoes() != null
                        && r.getQuantidadeReutilizacoes() > 0)
                .count();

        long totalReutilizacoes = respostas.stream()
                .mapToLong(r -> r.getQuantidadeReutilizacoes() == null
                        ? 0 : r.getQuantidadeReutilizacoes())
                .sum();

        double percentualDisponivel = totalRespostas == 0
                ? 0
                : respostasDisponiveis * 100.0 / totalRespostas;

        return new IndicadoresQualidadeDTO(
                empresaId,
                totalRespostas,
                respostasDisponiveis,
                respostasSeguras,
                respostasReutilizadas,
                totalReutilizacoes,
                percentualDisponivel
        );
    }

    public SustentabilidadeDTO consultarSustentabilidade(
            Long empresaId
    ) {

        validarEmpresa(empresaId);
        EconomiaReutilizacaoDTO economia =
                consultarEconomia(empresaId);

        return new SustentabilidadeDTO(
                economia.empresaId(),
                economia.chamadasEvitadas(),
                economia.tokensEconomizados(),
                economia.custoEstimadoEvitado(),
                economia.percentualReutilizacao()
        );
    }
    private void validarEmpresa(Long empresaId) {
        if (empresaAtual != null) {
            empresaAtual.validar(empresaId);
        }
    }
}
