package br.com.hestia.empresa.dto;

import br.com.hestia.empresa.model.Empresa;

public record EmpresaResponseDTO(
        Long id,
        String nome,
        String cnpj,
        String configuracoesGerais,
        Double orcamento
) {
    public static EmpresaResponseDTO from(Empresa empresa) {
        return new EmpresaResponseDTO(
                empresa.getId(), empresa.getNome(), empresa.getCnpj(),
                empresa.getConfiguracoesGerais(), empresa.getOrcamento()
        );
    }
}
