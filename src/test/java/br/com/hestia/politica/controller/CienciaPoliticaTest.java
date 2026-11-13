package br.com.hestia.politica.controller;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import br.com.hestia.usuario.model.Usuario;
import br.com.hestia.empresa.model.Empresa;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
class CienciaPoliticaTest {
 @Test void preservaCienciaPorVersaoSemDuplicarERejeitaOutraEmpresa(){
  var jdbc=new JdbcTemplate(new DriverManagerDataSource("jdbc:h2:mem:ciencia_"+System.nanoTime()+";MODE=PostgreSQL;DB_CLOSE_DELAY=-1","sa",""));
  jdbc.execute("CREATE TABLE usuarios(id BIGINT,empresa_id BIGINT)");
  jdbc.execute("CREATE TABLE politicas_uso(id BIGINT,empresa_id BIGINT,revisao INT,ativa BOOLEAN,excluida BOOLEAN)");
  jdbc.execute("CREATE TABLE politica_uso_versoes(id BIGINT,politica_uso_id BIGINT,revisao INT,titulo VARCHAR,conteudo VARCHAR,versao VARCHAR)");
  jdbc.execute("CREATE TABLE ciencias_politica(id BIGSERIAL PRIMARY KEY,empresa_id BIGINT,usuario_id BIGINT,politica_versao_id BIGINT,registrada_em TIMESTAMP,UNIQUE(usuario_id,politica_versao_id))");
  jdbc.update("INSERT INTO usuarios VALUES(1,10)");jdbc.update("INSERT INTO politicas_uso VALUES(2,10,1,true,false),(3,20,1,true,false)");
  jdbc.update("INSERT INTO politica_uso_versoes VALUES(5,2,1,'Política','Texto','1'),(6,3,1,'Outra','Texto','1')");
  var u=mock(Usuario.class);var e=mock(Empresa.class);when(u.getId()).thenReturn(1L);when(u.getEmpresa()).thenReturn(e);when(e.getId()).thenReturn(10L);
  var c=new CienciaPoliticaController(jdbc);c.registrar(u,5L);c.registrar(u,5L);
  assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM ciencias_politica",Integer.class));
  assertThrows(org.springframework.web.server.ResponseStatusException.class,()->c.registrar(u,6L));
  jdbc.update("UPDATE politicas_uso SET revisao=2 WHERE id=2");jdbc.update("INSERT INTO politica_uso_versoes VALUES(7,2,2,'Política','Novo texto','2')");
  c.registrar(u,7L);assertEquals(2,jdbc.queryForObject("SELECT COUNT(*) FROM ciencias_politica",Integer.class));
  assertThrows(org.springframework.web.server.ResponseStatusException.class,()->c.registrar(u,5L));
 }
}
