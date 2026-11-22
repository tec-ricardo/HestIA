package br.com.hestia.indicadores.maturidade;
import br.com.hestia.usuario.model.*;
import br.com.hestia.empresa.model.Empresa;
import br.com.hestia.departamento.model.Departamento;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import java.util.*;
import java.time.LocalDate;
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
    @Test void acompanhaSomenteEmpresaEDepartamentoComDadosReaisDoRecorte() {
        var jdbc = new JdbcTemplate(new DriverManagerDataSource(
            "jdbc:h2:mem:"+UUID.randomUUID()+";DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE","sa",""));
        jdbc.execute("CREATE TABLE departamentos(id BIGINT,empresa_id BIGINT,nome VARCHAR(100))");
        jdbc.execute("CREATE TABLE usuarios(id BIGINT,empresa_id BIGINT,departamento_id BIGINT,nome VARCHAR(100),ativo BOOLEAN,perfil VARCHAR(40))");
        jdbc.execute("CREATE TABLE progressos_usuario(usuario_id BIGINT,xp_total INT)");
        jdbc.execute("CREATE TABLE registros_uso_ia(id BIGINT,usuario_id BIGINT,tokens_total INT,status_execucao VARCHAR(40),data_hora TIMESTAMP,ferramenta_id BIGINT,finalidade VARCHAR(40))");
        jdbc.execute("CREATE TABLE politica_uso_versoes(id BIGINT,empresa_id BIGINT,politica_uso_id BIGINT)");
        jdbc.execute("CREATE TABLE avaliacoes_conformidade(registro_uso_id BIGINT,politica_versao_id BIGINT,politica_uso_id BIGINT,status VARCHAR(40),data_avaliacao TIMESTAMP,regra_codigo VARCHAR(40),regra_versao VARCHAR(40),motivo_preservado VARCHAR(40),regra_preservada VARCHAR(40),limitacoes_preservadas VARCHAR(40))");
        jdbc.execute("CREATE TABLE movimentos_experiencia(usuario_id BIGINT,acao VARCHAR(40),pontos INT,data_hora TIMESTAMP)");
        jdbc.execute("CREATE TABLE interacoes_rag(empresa_id BIGINT,usuario_id BIGINT,criada_em TIMESTAMP,estado VARCHAR(40))");
        jdbc.execute("CREATE TABLE politicas_uso(empresa_id BIGINT,excluida BOOLEAN)");
        jdbc.execute("CREATE TABLE ferramentas_ia(id BIGINT,empresa_id BIGINT,nome VARCHAR(100))");
        jdbc.update("INSERT INTO departamentos VALUES(100,10,'Equipe A'),(101,10,'Equipe B'),(200,20,'Outra empresa')");
        jdbc.update("INSERT INTO usuarios VALUES(1,10,100,'Ana',TRUE,'FUNCIONARIO'),(2,10,101,'Bia',TRUE,'FUNCIONARIO'),(3,20,200,'Carla',TRUE,'FUNCIONARIO')");
        jdbc.update("INSERT INTO progressos_usuario VALUES(1,15),(2,25),(3,40)");
        jdbc.update("INSERT INTO ferramentas_ia VALUES(1,10,'Assistente A'),(2,20,'Assistente B')");
        jdbc.update("INSERT INTO registros_uso_ia VALUES(1,1,50,'SUCESSO',CURRENT_TIMESTAMP,1,'PESQUISA'),(2,2,500,'SUCESSO',CURRENT_TIMESTAMP,1,'PESQUISA'),(3,3,5000,'SUCESSO',CURRENT_TIMESTAMP,2,'GERACAO_CONTEUDO')");
        jdbc.update("INSERT INTO registros_uso_ia VALUES(4,1,20,'SUCESSO',?,1,'PESQUISA')",java.sql.Timestamp.valueOf(LocalDate.now().minusDays(30).atStartOfDay()));
        jdbc.update("INSERT INTO movimentos_experiencia VALUES(1,'REUTILIZACAO_RESPOSTA',15,CURRENT_TIMESTAMP)");
        jdbc.update("INSERT INTO interacoes_rag VALUES(10,1,CURRENT_TIMESTAMP,'SEM_CONTEUDO_AUTORIZADO')");
        var evidencias=new EvidenciasAiLiteracyRepository(jdbc){
            @Override List<Tentativa> buscar(Long e,Long d,LocalDate i,LocalDate f){
                assertEquals(10L,e);assertEquals(100L,d);return List.of();
            }
        };
        var e=new Empresa();e.setId(10L);e.setNome("Empresa A");
        var dep=new Departamento();dep.setId(100L);dep.setNome("Equipe A");dep.setEmpresa(e);
        var autor=new Usuario();autor.setEmpresa(e);autor.setDepartamento(dep);autor.setPerfil(PerfilUsuario.GESTOR);
        var res=new AcompanhamentoColaboradoresController(jdbc,evidencias).listar(autor);
        var pessoas=(List<?>)res.get("colaboradores");
        assertEquals(1,pessoas.size());
        var ana=(Map<?,?>)pessoas.getFirst();
        assertEquals("Ana",ana.get("nome"));assertEquals(50L,((Number)ana.get("tokens")).longValue());
        assertEquals(1L,((Number)ana.get("reutilizacoes")).longValue());
        assertEquals(1L,((Number)ana.get("semFonte")).longValue());
        assertNull(ana.get("taxaConformidade"));
        assertNull(ana.get("aiLiteracy"));
        assertEquals(1,((List<?>)res.get("evolucao")).size());
        var ferramenta=(Map<?,?>)((List<?>)res.get("ferramentasUso")).getFirst();
        assertEquals("Assistente A",ferramenta.get("nome"));assertEquals(1L,((Number)ferramenta.get("usos")).longValue());
        assertEquals(1L,((Number)((Map<?,?>)res.get("anterior")).get("usos")).longValue());
        assertThrows(ResponseStatusException.class,()->new AcompanhamentoColaboradoresController(jdbc,evidencias).listar(autor,30,101L));
        assertThrows(ResponseStatusException.class,()->new AcompanhamentoColaboradoresController(jdbc,evidencias).listar(autor,15,null));
    }
}
