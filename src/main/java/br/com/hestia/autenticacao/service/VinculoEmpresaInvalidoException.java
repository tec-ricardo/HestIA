package br.com.hestia.autenticacao.service;

public class VinculoEmpresaInvalidoException extends RuntimeException {
    public VinculoEmpresaInvalidoException() {
        super("Usuário sem vínculo válido com uma empresa");
    }
}
