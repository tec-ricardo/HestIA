package br.com.hestia.notificacao.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;
import java.time.Duration;

@ConfigurationProperties("hestia.alert-service")
public record AlertServiceProperties(URI baseUrl, Duration timeout) {
}
