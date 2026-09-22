package br.com.hestia.auditoria.config;

import br.com.hestia.auditoria.service.AuditoriaService;
import br.com.hestia.usuario.model.Usuario;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuditoriaInterceptor implements HandlerInterceptor {
    private final AuditoriaService service;
    public AuditoriaInterceptor(AuditoriaService service) { this.service = service; }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception exception) {
        Usuario usuario = (Usuario) request.getAttribute("usuarioAutenticado");
        if (usuario != null) {
            service.registrar(usuario, request.getMethod(), request.getRequestURI(),
                    response.getStatus(), request.getRemoteAddr());
        }
    }
}
