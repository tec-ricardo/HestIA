package br.com.hestia.notificacao.client;

import br.com.hestia.notificacao.dto.CriarNotificacaoRequest;
import br.com.hestia.notificacao.exception.AlertServiceException;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.net.InetSocketAddress;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AlertServiceClientTest {

    private HttpServer server;

    @AfterEach
    void stop() {
        if (server != null) server.stop(0);
    }

    @Test
    void criaNotificacaoPeloContratoHttp() throws Exception {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/api/v1/notifications", exchange -> {
            var body = """
                    {"id":"d94ba844-c6cf-4ea9-a772-da412eb7377b","empresaId":1,
                    "origem":"consumo","tipo":"limite","valor":90.0,"mensagem":"Alerta",
                    "idempotencyKey":"evento-001","nivel":"CRITICAL","status":"CREATED",
                    "createdAt":"2026-09-02T12:00:00Z","correlationId":"corr-1"}
                    """;
            exchange.getResponseHeaders().add("content-type", "application/json");
            exchange.sendResponseHeaders(201, body.getBytes().length);
            exchange.getResponseBody().write(body.getBytes());
            exchange.close();
        });
        server.start();
        var client = new AlertServiceClient(RestClient.builder()
                .baseUrl("http://127.0.0.1:" + server.getAddress().getPort()).build());

        var response = client.criar(new CriarNotificacaoRequest(
                1L, "consumo", "limite", 90.0, "Alerta", "evento-001"
        ), "corr-1");

        assertThat(response.nivel()).isEqualTo("CRITICAL");
        assertThat(response.correlationId()).isEqualTo("corr-1");
    }

    @Test
    void converteFalhaExternaEmErroControlado() {
        var client = new AlertServiceClient(RestClient.builder()
                .baseUrl("http://127.0.0.1:1").build());

        assertThatThrownBy(() -> client.criar(new CriarNotificacaoRequest(
                1L, "consumo", "limite", 90.0, "Alerta", "evento-002"
        ), "corr-2"))
                .isInstanceOf(AlertServiceException.class)
                .hasMessageContaining("indisponível");
    }
}
