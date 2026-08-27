package br.com.hestia.departamento.service;

import br.com.hestia.departamento.dto.DepartamentoDTO;
import br.com.hestia.departamento.model.Departamento;
import br.com.hestia.departamento.repository.DepartamentoRepository;
import br.com.hestia.empresa.model.Empresa;
import br.com.hestia.empresa.repository.EmpresaRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DepartamentoService {

    private final DepartamentoRepository departamentoRepository;
    private final EmpresaRepository empresaRepository;

    public DepartamentoService(
            DepartamentoRepository departamentoRepository,
            EmpresaRepository empresaRepository) {

        this.departamentoRepository = departamentoRepository;
        this.empresaRepository = empresaRepository;
    }

    public Departamento cadastrar(DepartamentoDTO dto) {

        Empresa empresa = empresaRepository
                .findById(dto.getEmpresaId())
                .orElseThrow(() ->
                        new RuntimeException("Empresa não encontrada"));

        Departamento departamento = new Departamento();

        departamento.setNome(dto.getNome());
        departamento.setResponsavel(dto.getResponsavel());
        departamento.setEstruturaHierarquica(dto.getEstruturaHierarquica());
        departamento.setEmpresa(empresa);

        return departamentoRepository.save(departamento);
    }

    public List<Departamento> listarTodos() {
        return departamentoRepository.findAll();
    }

    public Departamento buscarPorId(Long id) {
        return departamentoRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Departamento não encontrado"));
    }

    public Departamento atualizar(Long id, DepartamentoDTO dto) {

        Departamento departamento = buscarPorId(id);

        Empresa empresa = empresaRepository
                .findById(dto.getEmpresaId())
                .orElseThrow(() ->
                        new RuntimeException("Empresa não encontrada"));

        departamento.setNome(dto.getNome());
        departamento.setResponsavel(dto.getResponsavel());
        departamento.setEstruturaHierarquica(dto.getEstruturaHierarquica());
        departamento.setEmpresa(empresa);

        return departamentoRepository.save(departamento);
    }

    public void excluir(Long id) {

        Departamento departamento = buscarPorId(id);

        departamentoRepository.delete(departamento);
    }
}