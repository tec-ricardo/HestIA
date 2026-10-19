package br.com.hestia.autenticacao.contexto;

public record ContextoEmpresa(Long usuarioId, Long empresaId) {
    public ContextoEmpresa {
        if (usuarioId == null || empresaId == null) {
            throw new IllegalArgumentException("Usuário e empresa são obrigatórios");
        }
    }
}
