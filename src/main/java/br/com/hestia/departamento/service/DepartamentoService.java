package br.com.hestia.departamento.service;

import br.com.hestia.auditoria.service.AuditoriaService;
import br.com.hestia.departamento.dto.DepartamentoDTO;
import br.com.hestia.departamento.model.Departamento;
import br.com.hestia.departamento.repository.DepartamentoRepository;
import br.com.hestia.empresa.model.Empresa;
import br.com.hestia.empresa.repository.EmpresaRepository;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@Service
public class DepartamentoService {

    private static final String NOME_OBRIGATORIO =
            "O nome do departamento é obrigatório";
    private static final String EMPRESA_NAO_ENCONTRADA =
            "Empresa não encontrada";
    private static final String DEPARTAMENTO_DUPLICADO =
            "Já existe um departamento com esse nome na empresa";

    private final DepartamentoRepository departamentoRepository;
    private final EmpresaRepository empresaRepository;
    private final AuditoriaService auditoriaService;

    @Autowired
    public DepartamentoService(
            DepartamentoRepository departamentoRepository,
            EmpresaRepository empresaRepository,
            AuditoriaService auditoriaService) {

        this.departamentoRepository = departamentoRepository;
        this.empresaRepository = empresaRepository;
        this.auditoriaService = auditoriaService;
    }

    public DepartamentoService(
            DepartamentoRepository departamentoRepository,
            EmpresaRepository empresaRepository) {
        this(departamentoRepository, empresaRepository, null);
    }

    public Departamento cadastrar(DepartamentoDTO dto) {
        String nome = validarENormalizarNome(dto);
        Empresa empresa = buscarEmpresaOuFalhar(dto.getEmpresaId());

        validarDuplicidade(empresa.getId(), nome);

        Departamento departamento = criarDepartamento(dto, nome, empresa);
        Departamento salvo = departamentoRepository.save(departamento);
        registrar("POST", salvo.getId());
        return salvo;
    }

    private String validarENormalizarNome(DepartamentoDTO dto) {
        if (dto == null || dto.getNome() == null || dto.getNome().isBlank()) {
            throw new IllegalArgumentException(NOME_OBRIGATORIO);
        }
        return dto.getNome().trim();
    }

    private Empresa buscarEmpresaOuFalhar(Long empresaId) {
        return empresaRepository.findById(empresaId)
                .orElseThrow(() -> new RuntimeException(EMPRESA_NAO_ENCONTRADA));
    }

    private void validarDuplicidade(Long empresaId, String nome) {
        if (departamentoRepository
                .existsByEmpresaIdAndNomeIgnoreCase(empresaId, nome)) {
            throw new IllegalArgumentException(DEPARTAMENTO_DUPLICADO);
        }
    }

    private Departamento criarDepartamento(
            DepartamentoDTO dto,
            String nome,
            Empresa empresa) {

        Departamento departamento = new Departamento();
        departamento.setNome(nome);
        departamento.setResponsavel(dto.getResponsavel());
        departamento.setEstruturaHierarquica(dto.getEstruturaHierarquica());
        departamento.setEmpresa(empresa);
        return departamento;
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
                        new RuntimeException(EMPRESA_NAO_ENCONTRADA));

        departamento.setNome(dto.getNome());
        departamento.setResponsavel(dto.getResponsavel());
        departamento.setEstruturaHierarquica(dto.getEstruturaHierarquica());
        departamento.setEmpresa(empresa);

        Departamento salvo = departamentoRepository.save(departamento);
        registrar("PUT", salvo.getId());
        return salvo;
    }

    public void excluir(Long id) {

        Departamento departamento = buscarPorId(id);

        departamentoRepository.delete(departamento);
        registrar("DELETE", id);
    }

    private void registrar(String metodo, Long id) {
        if (auditoriaService != null) {
            auditoriaService.registrarOperacao(metodo, "/departamentos/" + id, 200);
        }
    }
}
