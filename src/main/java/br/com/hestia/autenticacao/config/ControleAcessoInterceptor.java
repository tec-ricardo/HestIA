package br.com.hestia.autenticacao.config;

import br.com.hestia.autenticacao.model.SessaoUsuario;
import br.com.hestia.autenticacao.service.AutenticacaoService;
import br.com.hestia.autenticacao.service.CredenciaisInvalidasException;
import br.com.hestia.usuario.model.PerfilUsuario;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
public class ControleAcessoInterceptor implements HandlerInterceptor {
    private final AutenticacaoService autenticacaoService;
    public ControleAcessoInterceptor(AutenticacaoService autenticacaoService) {
        this.autenticacaoService = autenticacaoService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        if ("OPTIONS".equals(request.getMethod())) return true;
        try {
            SessaoUsuario sessao = autenticacaoService.validar(bearer(request.getHeader("Authorization")));
            PerfilUsuario perfil = sessao.getUsuario().getPerfil();
            if (!permitido(request.getRequestURI(), perfil)) {
                responder(response, 403, "FORBIDDEN", "Seu perfil não permite acessar este recurso");
                return false;
            }
            request.setAttribute("usuarioAutenticado", sessao.getUsuario());
            return true;
        } catch (CredenciaisInvalidasException exception) {
            responder(response, 401, "UNAUTHORIZED", exception.getMessage());
            return false;
        }
    }

    private boolean permitido(String rota, PerfilUsuario perfil) {
        if (rota.startsWith("/usuarios") || rota.startsWith("/empresas")
                || rota.startsWith("/departamentos") || rota.startsWith("/politicas")) {
            return perfil == PerfilUsuario.ADMIN;
        }
        if (rota.startsWith("/avaliacoes-conformidade")) {
            return perfil == PerfilUsuario.ADMIN || perfil == PerfilUsuario.GESTOR;
        }
        return true;
    }

    private String bearer(String authorization) {
        return authorization != null && authorization.startsWith("Bearer ")
                ? authorization.substring(7).trim() : null;
    }

    private void responder(HttpServletResponse response, int status, String codigo, String mensagem) throws IOException {
        response.setStatus(status);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"code\":\"" + codigo + "\",\"message\":\"" + mensagem + "\"}");
    }
}
