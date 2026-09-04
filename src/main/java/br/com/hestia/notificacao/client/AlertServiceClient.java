package br.com.hestia.notificacao.client;

import br.com.hestia.notificacao.dto.CriarNotificacaoRequest;
import br.com.hestia.notificacao.dto.NotificacaoResponse;
import br.com.hestia.notificacao.exception.AlertServiceException;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.UUID;

@Component
public class AlertServiceClient {

    private final RestClient restClient;

    public AlertServiceClient(RestClient alertServiceRestClient) {
        this.restClient = alertServiceRestClient;
    }

    public NotificacaoResponse criar(CriarNotificacaoRequest request, String correlationId) {
        try {
            return restClient.post()
                    .uri("/api/v1/notifications")
                    .header("x-correlation-id", correlationId)
                    .body(request)
                    .retrieve()
                    .body(NotificacaoResponse.class);
        } catch (RestClientException exception) {
            throw new AlertServiceException("Alert Service indisponível ou respondeu de forma inválida", exception);
        }
    }

    public NotificacaoResponse buscar(UUID id) {
        try {
            return restClient.get()
                    .uri("/api/v1/notifications/{id}", id)
                    .retrieve()
                    .body(NotificacaoResponse.class);
        } catch (RestClientException exception) {
            throw new AlertServiceException("Não foi possível consultar o Alert Service", exception);
        }
    }

    public List<NotificacaoResponse> listarPorEmpresa(Long empresaId) {
        try {
            return restClient.get()
                    .uri(builder -> builder.path("/api/v1/notifications")
                            .queryParam("empresaId", empresaId).build())
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
        } catch (RestClientException exception) {
            throw new AlertServiceException("Não foi possível listar notificações", exception);
        }
    }
}
