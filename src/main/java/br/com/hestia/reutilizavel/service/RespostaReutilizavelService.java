package br.com.hestia.reutilizacao.service;

import br.com.hestia.reutilizacao.dto.RespostaReutilizavelDTO;
import br.com.hestia.reutilizacao.model.RespostaReutilizavel;
import br.com.hestia.reutilizacao.repository.RespostaReutilizavelRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RespostaReutilizavelService {

    private final RespostaReutilizavelRepository repository;

    public RespostaReutilizavelService(
            RespostaReutilizavelRepository repository
    ) {
        this.repository = repository;
    }

    public RespostaReutilizavel cadastrar(RespostaReutilizavelDTO dto) {

        RespostaReutilizavel resposta = new RespostaReutilizavel();

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

    public List<RespostaReutilizavel> listarPorEmpresa(Long empresaId) {
        return repository.findByEmpresaId(empresaId);
    }

    public List<RespostaReutilizavel> listarReutilizaveisPorEmpresa(
            Long empresaId
    ) {
        return repository.findByEmpresaIdAndReutilizavelTrue(empresaId);
    }

    public RespostaReutilizavel buscarPorId(Long id) {
        return repository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Resposta reutilizável não encontrada"
                        )
                );
    }

    public RespostaReutilizavel registrarReutilizacao(Long id) {

        RespostaReutilizavel resposta = buscarPorId(id);

        resposta.registrarReutilizacao();

        return repository.save(resposta);
    }
}