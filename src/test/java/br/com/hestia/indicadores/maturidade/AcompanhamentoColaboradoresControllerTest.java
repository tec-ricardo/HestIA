package br.com.hestia.indicadores.maturidade;
import br.com.hestia.usuario.model.*;
import br.com.hestia.empresa.model.Empresa;
import br.com.hestia.departamento.model.Departamento;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;

class AcompanhamentoColaboradoresControllerTest {
    @Test void funcionarioNaoConsultaPainel() {
        var u=new Usuario(); u.setPerfil(PerfilUsuario.FUNCIONARIO);
        var e=new Empresa(); e.setId(1L); u.setEmpresa(e);
        assertThrows(ResponseStatusException.class,()->AcompanhamentoColaboradoresController.departamentoAutorizado(u));
    }
    @Test void gestorSemDepartamentoNaoRecebeEscopoGlobal() {
        var u=new Usuario(); u.setPerfil(PerfilUsuario.GESTOR);
        var e=new Empresa(); e.setId(1L); u.setEmpresa(e);
        assertThrows(ResponseStatusException.class,()->AcompanhamentoColaboradoresController.departamentoAutorizado(u));
    }
    @Test void adminRestritoAEmpresaPodeConsultarTodosDepartamentos() {
        var u=new Usuario(); u.setPerfil(PerfilUsuario.ADMIN);
        var e=new Empresa(); e.setId(1L); u.setEmpresa(e);
        assertNull(AcompanhamentoColaboradoresController.departamentoAutorizado(u));
    }
}
