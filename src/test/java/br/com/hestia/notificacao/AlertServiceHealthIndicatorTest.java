package br.com.hestia.notificacao;
import br.com.hestia.notificacao.config.AlertServiceHealthIndicator;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;
import org.springframework.boot.health.contributor.Status;
import java.util.Map;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
class AlertServiceHealthIndicatorTest {
    @Test void distingueSaudeDeRespostaInvalidaEFalhaSemDetalhes(){
        var client=mock(RestClient.class,RETURNS_DEEP_STUBS);var indicador=new AlertServiceHealthIndicator(client);
        when(client.get().uri("/health").retrieve().body(Map.class)).thenReturn(Map.of("status","UP"));assertEquals(Status.UP,indicador.health().getStatus());
        when(client.get().uri("/health").retrieve().body(Map.class)).thenReturn(Map.of("status","OUTRO"));assertEquals(Status.DOWN,indicador.health().getStatus());
        when(client.get().uri("/health").retrieve().body(Map.class)).thenThrow(new IllegalStateException("dado protegido"));var falha=indicador.health();assertEquals(Status.DOWN,falha.getStatus());assertTrue(falha.getDetails().isEmpty());
    }
}
