package br.com.hestia.departamento.service;

import br.com.hestia.auditoria.model.ResultadoAuditoria;
import br.com.hestia.auditoria.model.TipoAcaoAuditoria;
import br.com.hestia.auditoria.service.LogAuditoriaService;
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
    private final LogAuditoriaService logAuditoriaService;

    public DepartamentoService(
            DepartamentoRepository departamentoRepository,
            EmpresaRepository empresaRepository,
            LogAuditoriaService logAuditoriaService
    ) {
        this.departamentoRepository =
                departamentoRepository;

        this.empresaRepository =
                empresaRepository;

        this.logAuditoriaService =
                logAuditoriaService;
    }

    public Departamento cadastrar(
            DepartamentoDTO dto
    ) {

        Empresa empresa =
                empresaRepository
                        .findById(dto.getEmpresaId())
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Empresa não encontrada"
                                )
                        );

        Departamento departamento =
                new Departamento();

        departamento.setNome(
                dto.getNome()
        );

        departamento.setResponsavel(
                dto.getResponsavel()
        );

        departamento.setEstruturaHierarquica(
                dto.getEstruturaHierarquica()
        );

        departamento.setEmpresa(empresa);

        Departamento departamentoSalvo =
                departamentoRepository
                        .save(departamento);

        logAuditoriaService.registrar(
                null,
                TipoAcaoAuditoria.CRIAR,
                "DEPARTAMENTO",
                departamentoSalvo.getId(),
                "Departamento cadastrado",
                ResultadoAuditoria.SUCESSO,
                null,
                dadosDepartamento(
                        departamentoSalvo
                )
        );

        return departamentoSalvo;
    }

    public List<Departamento> listarTodos() {
        return departamentoRepository.findAll();
    }

    public Departamento buscarPorId(Long id) {

        return departamentoRepository
                .findById(id)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Departamento não encontrado"
                        )
                );
    }

    public Departamento atualizar(
            Long id,
            DepartamentoDTO dto
    ) {

        Departamento departamento =
                buscarPorId(id);

        String dadosAnteriores =
                dadosDepartamento(departamento);

        Empresa empresa =
                empresaRepository
                        .findById(dto.getEmpresaId())
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Empresa não encontrada"
                                )
                        );

        departamento.setNome(
                dto.getNome()
        );

        departamento.setResponsavel(
                dto.getResponsavel()
        );

        departamento.setEstruturaHierarquica(
                dto.getEstruturaHierarquica()
        );

        departamento.setEmpresa(empresa);

        Departamento departamentoSalvo =
                departamentoRepository
                        .save(departamento);

        logAuditoriaService.registrar(
                null,
                TipoAcaoAuditoria.ALTERAR,
                "DEPARTAMENTO",
                departamentoSalvo.getId(),
                "Departamento atualizado",
                ResultadoAuditoria.SUCESSO,
                dadosAnteriores,
                dadosDepartamento(
                        departamentoSalvo
                )
        );

        return departamentoSalvo;
    }

    public void excluir(Long id) {

        Departamento departamento =
                buscarPorId(id);

        String dadosAnteriores =
                dadosDepartamento(departamento);

        departamentoRepository
                .delete(departamento);

        logAuditoriaService.registrar(
                null,
                TipoAcaoAuditoria.EXCLUIR,
                "DEPARTAMENTO",
                id,
                "Departamento excluído",
                ResultadoAuditoria.SUCESSO,
                dadosAnteriores,
                null
        );
    }

    private String dadosDepartamento(
            Departamento departamento
    ) {

        Long empresaId = null;

        if (departamento.getEmpresa() != null) {
            empresaId =
                    departamento
                            .getEmpresa()
                            .getId();
        }

        return "nome="
                + departamento.getNome()
                + "; responsavel="
                + departamento.getResponsavel()
                + "; estruturaHierarquica="
                + departamento.getEstruturaHierarquica()
                + "; empresaId="
                + empresaId;
    }
}