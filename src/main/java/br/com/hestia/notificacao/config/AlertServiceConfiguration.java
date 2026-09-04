package br.com.hestia.notificacao.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(AlertServiceProperties.class)
public class AlertServiceConfiguration {

    @Bean
    RestClient alertServiceRestClient(AlertServiceProperties properties) {
        var factory = new JdkClientHttpRequestFactory();
        factory.setReadTimeout(properties.timeout());
        return RestClient.builder()
                .baseUrl(properties.baseUrl().toString())
                .requestFactory(factory)
                .build();
    }
}
