package br.com.hestia.registro;
import br.com.hestia.registro.controller.RelatorioUsoController;
import br.com.hestia.usuario.model.*;
import br.com.hestia.empresa.model.Empresa;
import br.com.hestia.departamento.model.Departamento;
import org.springframework.jdbc.core.JdbcTemplate;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
class RelatorioUsoTest {
 @Test void bloqueiaOutroDepartamentoAntesDaConsulta(){var jdbc=mock(JdbcTemplate.class);var u=mock(Usuario.class);var e=mock(Empresa.class);var d=mock(Departamento.class);when(u.getPerfil()).thenReturn(PerfilUsuario.GESTOR);when(u.getEmpresa()).thenReturn(e);when(e.getId()).thenReturn(10L);when(u.getDepartamento()).thenReturn(d);when(d.getId()).thenReturn(2L);assertThrows(org.springframework.web.server.ResponseStatusException.class,()->new RelatorioUsoController(jdbc).linhas(u,null,null,3L,null,null));verifyNoInteractions(jdbc);}
 @Test void protegeFormulasEAspas(){assertEquals("\"'=1+1\"",RelatorioUsoController.campo("=1+1"));assertEquals("\"a\"\"b,c\"",RelatorioUsoController.campo("a\"b,c"));assertEquals("",RelatorioUsoController.campo(null));}
}
