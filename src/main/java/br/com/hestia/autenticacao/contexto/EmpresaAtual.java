package br.com.hestia.autenticacao.contexto;

import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

@Component
@RequestScope
public class EmpresaAtual {
    private ContextoEmpresa contexto;

    public void definir(ContextoEmpresa contexto) {
        if (contexto == null) {
            throw new IllegalArgumentException("Contexto da empresa é obrigatório");
        }
        this.contexto = contexto;
    }

    public ContextoEmpresa obterContexto() {
        if (contexto == null) {
            throw new IllegalStateException("Contexto da empresa não foi definido");
        }
        return contexto;
    }

    public Long obterId() {
        return obterContexto().empresaId();
    }

    public void validar(Long empresaId) {
        if (empresaId == null || !obterId().equals(empresaId)) {
            throw new IllegalArgumentException("A operação não pertence à empresa autenticada");
        }
    }
}
