package br.com.hestia.empresa.service;

import br.com.hestia.auditoria.model.ResultadoAuditoria;
import br.com.hestia.auditoria.model.TipoAcaoAuditoria;
import br.com.hestia.auditoria.service.LogAuditoriaService;
import br.com.hestia.empresa.dto.EmpresaDTO;
import br.com.hestia.empresa.model.Empresa;
import br.com.hestia.empresa.repository.EmpresaRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EmpresaService {

    private final EmpresaRepository empresaRepository;
    private final LogAuditoriaService logAuditoriaService;

    public EmpresaService(
            EmpresaRepository empresaRepository,
            LogAuditoriaService logAuditoriaService
    ) {
        this.empresaRepository = empresaRepository;
        this.logAuditoriaService = logAuditoriaService;
    }

    public Empresa cadastrar(EmpresaDTO dto) {

        var cnpj = normalizarCnpj(dto.getCnpj());

        if (empresaRepository.existsByCnpj(cnpj)) {
            throw new IllegalArgumentException(
                    "CNPJ já cadastrado"
            );
        }

        Empresa empresa = new Empresa();

        empresa.setNome(dto.getNome().trim());
        empresa.setCnpj(cnpj);
        empresa.setConfiguracoesGerais(
                dto.getConfiguracoesGerais()
        );
        empresa.setOrcamento(dto.getOrcamento());

        Empresa empresaSalva =
                empresaRepository.save(empresa);

        logAuditoriaService.registrar(
                null,
                TipoAcaoAuditoria.CRIAR,
                "EMPRESA",
                empresaSalva.getId(),
                "Empresa cadastrada",
                ResultadoAuditoria.SUCESSO,
                null,
                dadosEmpresa(empresaSalva)
        );

        return empresaSalva;
    }

    public List<Empresa> listarTodas() {
        return empresaRepository.findAll();
    }

    public Empresa buscarPorId(Long id) {

        return empresaRepository.findById(id)
                .orElseThrow(() ->
                        new java.util.NoSuchElementException(
                                "Empresa não encontrada"
                        )
                );
    }

    public Empresa atualizar(
            Long id,
            EmpresaDTO dto
    ) {

        Empresa empresa = buscarPorId(id);

        String dadosAnteriores =
                dadosEmpresa(empresa);

        var cnpj = normalizarCnpj(dto.getCnpj());

        if (empresaRepository
                .existsByCnpjAndIdNot(cnpj, id)) {

            throw new IllegalArgumentException(
                    "CNPJ já cadastrado"
            );
        }

        empresa.setNome(dto.getNome().trim());
        empresa.setCnpj(cnpj);
        empresa.setConfiguracoesGerais(
                dto.getConfiguracoesGerais()
        );
        empresa.setOrcamento(dto.getOrcamento());

        Empresa empresaSalva =
                empresaRepository.save(empresa);

        logAuditoriaService.registrar(
                null,
                TipoAcaoAuditoria.ALTERAR,
                "EMPRESA",
                empresaSalva.getId(),
                "Empresa atualizada",
                ResultadoAuditoria.SUCESSO,
                dadosAnteriores,
                dadosEmpresa(empresaSalva)
        );

        return empresaSalva;
    }

    public void excluir(Long id) {

        Empresa empresa = buscarPorId(id);

        String dadosAnteriores =
                dadosEmpresa(empresa);

        empresaRepository.delete(empresa);

        logAuditoriaService.registrar(
                null,
                TipoAcaoAuditoria.EXCLUIR,
                "EMPRESA",
                id,
                "Empresa excluída",
                ResultadoAuditoria.SUCESSO,
                dadosAnteriores,
                null
        );
    }

    private String normalizarCnpj(String cnpj) {
        return cnpj.replaceAll("\\D", "");
    }

    private String dadosEmpresa(
            Empresa empresa
    ) {

        return "nome=" + empresa.getNome()
                + "; cnpj=" + empresa.getCnpj()
                + "; configuracoesGerais="
                + empresa.getConfiguracoesGerais()
                + "; orcamento="
                + empresa.getOrcamento();
    }
}