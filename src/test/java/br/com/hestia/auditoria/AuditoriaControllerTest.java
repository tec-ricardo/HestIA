package br.com.hestia.auditoria;

import br.com.hestia.auditoria.controller.AuditoriaController;
import br.com.hestia.auditoria.service.AuditoriaService;
import br.com.hestia.empresa.model.Empresa;
import br.com.hestia.usuario.model.PerfilUsuario;
import br.com.hestia.usuario.model.Usuario;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class AuditoriaControllerTest {
    @Test
    void limitaConsultaAEmpresaAutenticada() {
        var service = mock(AuditoriaService.class);
        var controller = new AuditoriaController(service);

        controller.listar(admin(1L), null, null, null, null, null);
        controller.listar(admin(2L), 2L, null, null, null, null);

        verify(service).listar(1L, null, null, null, null);
        verify(service).listar(2L, null, null, null, null);
    }

    @Test
    void bloqueiaConsultaDeOutraEmpresaAntesDeConsultarRegistros() {
        var service = mock(AuditoriaService.class);
        var controller = new AuditoriaController(service);

        assertThatThrownBy(() -> controller.listar(admin(1L), 2L, null, null, null, null))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        erro -> assertThat(erro.getStatusCode().value()).isEqualTo(403));
        verifyNoInteractions(service);
    }

    private Usuario admin(Long empresaId) {
        var empresa = mock(Empresa.class);
        when(empresa.getId()).thenReturn(empresaId);
        var usuario = new Usuario();
        usuario.setPerfil(PerfilUsuario.ADMIN);
        usuario.setEmpresa(empresa);
        return usuario;
    }
}
