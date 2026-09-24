package br.com.hestia.reutilizavel.service;

import br.com.hestia.reutilizavel.dto.RespostaReutilizavelDTO;
import br.com.hestia.reutilizavel.dto.EconomiaReutilizacaoDTO;
import br.com.hestia.gamificacao.dto.RegistroAcaoDTO;
import br.com.hestia.gamificacao.model.AcaoExperiencia;
import br.com.hestia.gamificacao.service.GamificacaoService;
import br.com.hestia.reutilizavel.model.RespostaReutilizavel;
import br.com.hestia.reutilizavel.repository.RespostaReutilizavelRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
public class RespostaReutilizavelService {

    static final int LIMITE_RECOMPENSAS_REUTILIZACAO = 10;

    private final RespostaReutilizavelRepository repository;
    private final GamificacaoService gamificacaoService;

    public RespostaReutilizavelService(
            RespostaReutilizavelRepository repository,
            GamificacaoService gamificacaoService
    ) {
        this.repository = repository;
        this.gamificacaoService = gamificacaoService;
    }

    public RespostaReutilizavel cadastrar(
            RespostaReutilizavelDTO dto
    ) {

        RespostaReutilizavel resposta =
                new RespostaReutilizavel();

        resposta.setUsuarioId(dto.getUsuarioId());
        resposta.setEmpresaId(dto.getEmpresaId());
        resposta.setPromptOriginal(dto.getPromptOriginal());
        resposta.setRespostaGerada(dto.getRespostaGerada());
        resposta.setEmbedding(dto.getEmbedding());
        resposta.setCategoria(dto.getCategoria());
        resposta.setModeloIA(dto.getModeloIA());
        resposta.setTokensEntrada(dto.getTokensEntrada());
        resposta.setTokensSaida(dto.getTokensSaida());

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
        return repository.findAll();
    }

    public List<RespostaReutilizavel> listarPorEmpresa(
            Long empresaId
    ) {
        return repository.findByEmpresaId(empresaId);
    }

    public List<RespostaReutilizavel>
    listarReutilizaveisPorEmpresa(
            Long empresaId
    ) {
        return repository
                .findByEmpresaIdAndReutilizavelTrue(
                        empresaId
                );
    }

    @Transactional(readOnly = true)
    public List<RespostaReutilizavel> buscarRespostasAnteriores(
            Long empresaId,
            String prompt
    ) {
        if (empresaId == null || empresaId <= 0
                || prompt == null || prompt.isBlank()) {
            throw new IllegalArgumentException(
                    "Informe uma empresa válida e o prompt para recuperar respostas anteriores."
            );
        }

        return repository
                .findTop10ByEmpresaIdAndReutilizavelTrueAndPromptOriginalContainingIgnoreCaseOrderByDataCriacaoDesc(
                        empresaId,
                        prompt.trim()
                );
    }

    @Transactional(readOnly = true)
    public List<RespostaReutilizavel> recuperarPerguntasSemelhantes(
            Long empresaId
    ) {

        if (empresaId == null || empresaId <= 0) {
            throw new IllegalArgumentException(
                    "Informe uma empresa válida para recuperar perguntas semelhantes."
            );
        }

        return repository
                .findByEmpresaIdAndReutilizavelTrueAndPossuiDadosSensiveisFalseAndEmbeddingIsNotNull(
                        empresaId
                );
    }

    public RespostaReutilizavel buscarPorId(Long id) {
        return repository.findById(id)
                .orElseThrow(() ->
                        new NoSuchElementException(
                                "Resposta reutilizável não encontrada."
                        )
                );
    }

    @Transactional
    public RespostaReutilizavel registrarReutilizacao(
            Long id
    ) {

        RespostaReutilizavel resposta =
                buscarPorId(id);

        if (!Boolean.TRUE.equals(resposta.getReutilizavel())) {
            throw new IllegalStateException(
                    "A resposta selecionada não está disponível para reutilização."
            );
        }

        resposta.registrarReutilizacao();
        RespostaReutilizavel salva = repository.save(resposta);
        if (salva.getUsuarioId() != null
                && salva.getQuantidadeReutilizacoes() <= LIMITE_RECOMPENSAS_REUTILIZACAO) {
            gamificacaoService.registrarAcao(new RegistroAcaoDTO(salva.getUsuarioId(),
                    AcaoExperiencia.REUTILIZACAO_RESPOSTA,
                    "REUTILIZACAO:" + salva.getId() + ":" + salva.getQuantidadeReutilizacoes()));
        }
        return salva;
    }

    public EconomiaReutilizacaoDTO consultarEconomia(Long empresaId) {
        List<RespostaReutilizavel> respostas = repository.findByEmpresaId(empresaId);
        long chamadas = respostas.stream().mapToLong(r -> r.getChamadasEvitadas()).sum();
        long tokens = respostas.stream().mapToLong(RespostaReutilizavel::getTokensEconomizados).sum();
        return new EconomiaReutilizacaoDTO(empresaId, chamadas, tokens);
    }
}
