package br.com.hestia.autenticacao.service;

public class CredenciaisInvalidasException extends RuntimeException {
    public CredenciaisInvalidasException(String mensagem) { super(mensagem); }
}
