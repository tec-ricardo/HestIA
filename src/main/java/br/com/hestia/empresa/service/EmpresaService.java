package br.com.hestia.empresa.service;

import br.com.hestia.auditoria.service.AuditoriaService;
import br.com.hestia.empresa.dto.EmpresaDTO;
import br.com.hestia.empresa.model.Empresa;
import br.com.hestia.empresa.repository.EmpresaRepository;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@Service
public class EmpresaService {

    private final EmpresaRepository empresaRepository;
    private final AuditoriaService auditoriaService;

    @Autowired
    public EmpresaService(EmpresaRepository empresaRepository, AuditoriaService auditoriaService) {
        this.empresaRepository = empresaRepository;
        this.auditoriaService = auditoriaService;
    }

    public EmpresaService(EmpresaRepository empresaRepository) {
        this(empresaRepository, null);
    }

    public Empresa cadastrar(EmpresaDTO dto) {
        var cnpj = normalizarCnpj(dto.getCnpj());
        if (empresaRepository.existsByCnpj(cnpj)) {
            throw new IllegalArgumentException("CNPJ já cadastrado");
        }

        Empresa empresa = new Empresa();

        empresa.setNome(dto.getNome().trim());
        empresa.setCnpj(cnpj);
        empresa.setConfiguracoesGerais(dto.getConfiguracoesGerais());
        empresa.setOrcamento(dto.getOrcamento());

        Empresa salva = empresaRepository.save(empresa);
        registrar("POST", salva.getId());
        return salva;
    }

    public List<Empresa> listarTodas() {
        return empresaRepository.findAll();
    }

    public Empresa buscarPorId(Long id) {
        return empresaRepository.findById(id)
                .orElseThrow(() -> new java.util.NoSuchElementException("Empresa não encontrada"));
    }

    public Empresa atualizar(Long id, EmpresaDTO dto) {
        Empresa empresa = buscarPorId(id);
        var cnpj = normalizarCnpj(dto.getCnpj());
        if (empresaRepository.existsByCnpjAndIdNot(cnpj, id)) {
            throw new IllegalArgumentException("CNPJ já cadastrado");
        }

        empresa.setNome(dto.getNome().trim());
        empresa.setCnpj(cnpj);
        empresa.setConfiguracoesGerais(dto.getConfiguracoesGerais());
        empresa.setOrcamento(dto.getOrcamento());

        Empresa salva = empresaRepository.save(empresa);
        registrar("PUT", salva.getId());
        return salva;
    }

    public void excluir(Long id) {
        Empresa empresa = buscarPorId(id);
        empresaRepository.delete(empresa);
        registrar("DELETE", id);
    }

    private String normalizarCnpj(String cnpj) {
        return cnpj.replaceAll("\\D", "");
    }

    private void registrar(String metodo, Long id) {
        if (auditoriaService != null) {
            auditoriaService.registrarOperacao(metodo, "/empresas/" + id, 200);
        }
    }
}
