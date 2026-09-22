package br.com.hestia.auditoria;

import br.com.hestia.auditoria.config.AuditoriaInterceptor;
import br.com.hestia.auditoria.service.AuditoriaService;
import br.com.hestia.usuario.model.Usuario;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import static org.mockito.Mockito.*;

class AuditoriaInterceptorTest {
    @Test
    void deveRegistrarAcaoDoUsuarioAutenticado() {
        AuditoriaService service = mock(AuditoriaService.class);
        Usuario usuario = mock(Usuario.class);
        var request = new MockHttpServletRequest("PUT", "/empresas/1/configuracoes");
        request.setRemoteAddr("127.0.0.1");
        request.setAttribute("usuarioAutenticado", usuario);
        var response = new MockHttpServletResponse();
        response.setStatus(200);

        new AuditoriaInterceptor(service).afterCompletion(request, response, new Object(), null);

        verify(service).registrar(usuario, "PUT", "/empresas/1/configuracoes", 200, "127.0.0.1");
    }
}
