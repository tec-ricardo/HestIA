package br.com.hestia.gamificacao;
import br.com.hestia.gamificacao.controller.DetalhamentoXpController;
import br.com.hestia.usuario.model.*;
import br.com.hestia.empresa.model.Empresa;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.web.server.ResponseStatusException;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class DetalhamentoXpControllerTest {
    JdbcTemplate jdbc;
    DetalhamentoXpController controller;
    @BeforeEach void preparar(){
        jdbc=new JdbcTemplate(new DriverManagerDataSource("jdbc:h2:mem:"+UUID.randomUUID()+";DB_CLOSE_DELAY=-1","sa",""));
        jdbc.execute("CREATE TABLE usuarios(id BIGINT, nome VARCHAR(100), empresa_id BIGINT, departamento_id BIGINT, ativo BOOLEAN)");
        jdbc.execute("CREATE TABLE progressos_usuario(usuario_id BIGINT,xp_total INT)");
        jdbc.execute("CREATE TABLE movimentos_experiencia(usuario_id BIGINT,acao VARCHAR(50),pontos INT)");
        jdbc.update("INSERT INTO usuarios VALUES(1,'Ana',10,100,TRUE),(2,'Bia',20,200,TRUE),(3,'Carla',10,100,TRUE)");
        jdbc.update("INSERT INTO progressos_usuario VALUES(1,25),(2,40),(3,15)");
        jdbc.update("INSERT INTO movimentos_experiencia VALUES(1,'USO_CONFORME',25),(2,'MICRO_TREINAMENTO',40),(3,'REUTILIZACAO_RESPOSTA',15)");
        controller=new DetalhamentoXpController(jdbc);
    }
    Usuario autor(){
        var u=new Usuario();u.setId(1L);u.setPerfil(PerfilUsuario.FUNCIONARIO);
        var e=new Empresa();e.setId(10L);u.setEmpresa(e);return u;
    }
    @Test void conciliaSaldoReal(){
        var resultado=controller.detalhar(1L,autor());
        assertEquals(true,resultado.get("conciliado"));assertEquals(25L,resultado.get("xpMovimentos"));
    }
    @Test void naoConsultaOutraEmpresa(){
        assertThrows(ResponseStatusException.class,()->controller.detalhar(2L,autor()));
    }
    @Test void colegaVisivelNoRankingTemSomenteTotais(){
        var r=controller.detalhar(3L,autor()); assertEquals(15L,r.get("xpMovimentos"));
    }
}
