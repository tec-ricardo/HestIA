package br.com.hestia.autenticacao.config;

import br.com.hestia.autenticacao.contexto.ContextoEmpresa;
import br.com.hestia.autenticacao.contexto.EmpresaAtual;
import br.com.hestia.autenticacao.model.SessaoUsuario;
import br.com.hestia.autenticacao.service.AutenticacaoService;
import br.com.hestia.autenticacao.service.CredenciaisInvalidasException;
import br.com.hestia.autenticacao.service.VinculoEmpresaInvalidoException;
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
    private final EmpresaAtual empresaAtual;

    public ControleAcessoInterceptor(AutenticacaoService autenticacaoService, EmpresaAtual empresaAtual) {
        this.autenticacaoService = autenticacaoService;
        this.empresaAtual = empresaAtual;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        if ("OPTIONS".equals(request.getMethod())) return true;
        try {
            SessaoUsuario sessao = autenticacaoService.validar(bearer(request.getHeader("Authorization")));
            Long usuarioId = sessao.getUsuario().getId();
            if (sessao.getUsuario().getEmpresa() == null || sessao.getUsuario().getEmpresa().getId() == null) {
                throw new VinculoEmpresaInvalidoException();
            }
            empresaAtual.definir(new ContextoEmpresa(usuarioId, sessao.getUsuario().getEmpresa().getId()));

            PerfilUsuario perfil = sessao.getUsuario().getPerfil();
            if (!permitido(request.getRequestURI(), perfil)) {
                responder(response, 403, "FORBIDDEN", "Seu perfil não permite acessar este recurso");
                return false;
            }
            request.setAttribute("usuarioAutenticado", sessao.getUsuario());
            return true;
        } catch (VinculoEmpresaInvalidoException exception) {
            responder(response, 403, "FORBIDDEN", exception.getMessage());
            return false;
        } catch (CredenciaisInvalidasException exception) {
            responder(response, 401, "UNAUTHORIZED", exception.getMessage());
            return false;
        }
    }

    private boolean permitido(String rota, PerfilUsuario perfil) {
        if (rota.startsWith("/usuarios") || rota.startsWith("/empresas")
                || rota.startsWith("/departamentos") || rota.startsWith("/politicas")
                || rota.startsWith("/auditoria")) {
            return perfil == PerfilUsuario.ADMIN;
        }
        if (rota.startsWith("/avaliacoes-conformidade") || rota.startsWith("/tratamentos-nao-conformidade")) {
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
