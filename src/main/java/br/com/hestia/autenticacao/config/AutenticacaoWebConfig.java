package br.com.hestia.autenticacao.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class AutenticacaoWebConfig implements WebMvcConfigurer {
    private final ControleAcessoInterceptor interceptor;
    public AutenticacaoWebConfig(ControleAcessoInterceptor interceptor) { this.interceptor = interceptor; }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(interceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/auth/**", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html", "/error");
    }
}
