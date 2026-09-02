package br.com.hestia.notificacao;

import br.com.hestia.notificacao.client.AlertServiceClient;
import br.com.hestia.notificacao.dto.CriarNotificacaoRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;

@EnabledIfEnvironmentVariable(named = "ALERT_E2E", matches = "true")
class AlertServiceEndToEndTest {

    private final AlertServiceClient client = new AlertServiceClient(
            RestClient.builder().baseUrl(System.getenv().getOrDefault(
                    "ALERT_SERVICE_URL", "http://127.0.0.1:3001"
            )).build()
    );

    @Test
    void enviaConsultaERepeteSemDuplicar() {
        var key = "e2e-" + System.currentTimeMillis();
        var request = new CriarNotificacaoRequest(
                42L, "teste-e2e", "limite", 95.0,
                "Teste de integracao HestIA", key
        );

        var first = client.criar(request, "e2e-correlation");
        var repeated = client.criar(request, "e2e-correlation");
        var fetched = client.buscar(first.id());
        var companyItems = client.listarPorEmpresa(42L);

        assertThat(first.nivel()).isEqualTo("CRITICAL");
        assertThat(repeated.id()).isEqualTo(first.id());
        assertThat(fetched.id()).isEqualTo(first.id());
        assertThat(companyItems).extracting(item -> item.id()).contains(first.id());
    }
}
