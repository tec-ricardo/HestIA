package br.com.hestia.empresa.service;

import br.com.hestia.empresa.dto.EmpresaDTO;
import br.com.hestia.empresa.model.Empresa;
import br.com.hestia.empresa.repository.EmpresaRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EmpresaService {

    private final EmpresaRepository empresaRepository;

    public EmpresaService(EmpresaRepository empresaRepository) {
        this.empresaRepository = empresaRepository;
    }

    public Empresa cadastrar(EmpresaDTO dto) {
        Empresa empresa = new Empresa();

        empresa.setNome(dto.getNome());
        empresa.setConfiguracoesGerais(dto.getConfiguracoesGerais());
        empresa.setPoliticas(dto.getPoliticas());
        empresa.setOrcamento(dto.getOrcamento());

        return empresaRepository.save(empresa);
    }

    public List<Empresa> listarTodas() {
        return empresaRepository.findAll();
    }

    public Empresa buscarPorId(Long id) {
        return empresaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Empresa não encontrada"));
    }

    public Empresa atualizar(Long id, EmpresaDTO dto) {
        Empresa empresa = buscarPorId(id);

        empresa.setNome(dto.getNome());
        empresa.setConfiguracoesGerais(dto.getConfiguracoesGerais());
        empresa.setPoliticas(dto.getPoliticas());
        empresa.setOrcamento(dto.getOrcamento());

        return empresaRepository.save(empresa);
    }

    public void excluir(Long id) {
        Empresa empresa = buscarPorId(id);
        empresaRepository.delete(empresa);
    }
}