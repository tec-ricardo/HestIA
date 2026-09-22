package br.com.hestia.configuracao.service;

import br.com.hestia.configuracao.dto.*;
import br.com.hestia.configuracao.model.ConfiguracaoEmpresa;
import br.com.hestia.configuracao.repository.ConfiguracaoEmpresaRepository;
import br.com.hestia.empresa.repository.EmpresaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.NoSuchElementException;

@Service
public class ConfiguracaoEmpresaService {
    private final ConfiguracaoEmpresaRepository repository;
    private final EmpresaRepository empresaRepository;
    public ConfiguracaoEmpresaService(ConfiguracaoEmpresaRepository repository, EmpresaRepository empresaRepository) {
        this.repository = repository; this.empresaRepository = empresaRepository;
    }

    @Transactional
    public ConfiguracaoEmpresa obterEntidade(Long empresaId) {
        return repository.findByEmpresaId(empresaId).orElseGet(() -> repository.save(new ConfiguracaoEmpresa(
                empresaRepository.findById(empresaId).orElseThrow(() -> new NoSuchElementException("Empresa não encontrada")))));
    }

    @Transactional
    public ConfiguracaoEmpresaResponseDTO consultar(Long empresaId) {
        return repository.findByEmpresaId(empresaId).map(ConfiguracaoEmpresaResponseDTO::from)
                .orElseGet(() -> ConfiguracaoEmpresaResponseDTO.from(obterEntidade(empresaId)));
    }

    @Transactional
    public ConfiguracaoEmpresaResponseDTO alterar(Long empresaId, ConfiguracaoEmpresaDTO dto) {
        ConfiguracaoEmpresa configuracao = obterEntidade(empresaId);
        configuracao.atualizar(dto.registrarUsosAutomaticamente(), dto.bloquearFerramentasNaoAprovadas(),
                dto.limiteCustoMensal(), dto.diasRetencaoAuditoria());
        return ConfiguracaoEmpresaResponseDTO.from(repository.save(configuracao));
    }
}
