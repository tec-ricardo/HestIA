package br.com.hestia.ferramenta.service;

import br.com.hestia.empresa.repository.EmpresaRepository;
import br.com.hestia.ferramenta.dto.FerramentaIACriacaoDTO;
import br.com.hestia.ferramenta.model.FerramentaIA;
import br.com.hestia.ferramenta.repository.FerramentaIARepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;

@Service
public class FerramentaIAService {

    private final FerramentaIARepository ferramentaRepository;
    private final EmpresaRepository empresaRepository;

    public FerramentaIAService(FerramentaIARepository ferramentaRepository,
                               EmpresaRepository empresaRepository) {
        this.ferramentaRepository = ferramentaRepository;
        this.empresaRepository = empresaRepository;
    }

    public FerramentaIA criar(FerramentaIACriacaoDTO dto) {
        var empresa = empresaRepository.findById(dto.empresaId())
                .orElseThrow(() -> new NoSuchElementException("Empresa não encontrada."));

        if (ferramentaRepository.existsByEmpresaIdAndNomeIgnoreCaseAndFornecedorIgnoreCase(
                dto.empresaId(), dto.nome(), dto.fornecedor())) {
            throw new IllegalArgumentException(
                    "Já existe uma ferramenta com esse nome e fornecedor nesta empresa.");
        }

        return ferramentaRepository.save(new FerramentaIA(
                dto.nome(), dto.fornecedor(), dto.descricao(), dto.tipo(),
                dto.finalidadeUso(), dto.urlAcesso(), dto.trataDadosPessoais(), empresa));
    }

    public List<FerramentaIA> listar(Long empresaId) {
        return empresaId == null
                ? ferramentaRepository.findAll()
                : ferramentaRepository.findByEmpresaId(empresaId);
    }

    public FerramentaIA buscar(Long id) {
        return ferramentaRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Ferramenta de IA não encontrada."));
    }

    public void excluir(Long id) {
        ferramentaRepository.delete(buscar(id));
    }
}
