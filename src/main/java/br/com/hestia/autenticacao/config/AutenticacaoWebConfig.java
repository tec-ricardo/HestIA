package br.com.hestia.autenticacao.config;

import br.com.hestia.auditoria.config.AuditoriaInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class AutenticacaoWebConfig implements WebMvcConfigurer {
    private final ControleAcessoInterceptor interceptor;
    private final AuditoriaInterceptor auditoriaInterceptor;
    public AutenticacaoWebConfig(ControleAcessoInterceptor interceptor,
                                 AuditoriaInterceptor auditoriaInterceptor) {
        this.interceptor = interceptor;
        this.auditoriaInterceptor = auditoriaInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(interceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/auth/**", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html", "/error");
        registry.addInterceptor(auditoriaInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/auth/**", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html", "/error");
    }
}
