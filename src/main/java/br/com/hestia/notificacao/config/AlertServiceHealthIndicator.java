package br.com.hestia.notificacao.config;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import java.util.Map;
@Component("alertas")
@ConditionalOnProperty(name="hestia.alert-service.health-enabled",havingValue="true")
public class AlertServiceHealthIndicator implements HealthIndicator {
    private final RestClient client;
    public AlertServiceHealthIndicator(RestClient alertServiceRestClient){client=alertServiceRestClient;}
    @Override public Health health(){
        try{var r=client.get().uri("/health").retrieve().body(Map.class);return r!=null&&"UP".equals(r.get("status"))?Health.up().build():Health.down().build();}
        catch(RuntimeException e){return Health.down().build();}
    }
}
