package br.com.hestia.notificacao.exception;

public class AlertServiceException extends RuntimeException {
    public AlertServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
