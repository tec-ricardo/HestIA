package br.com.hestia.registro;
import br.com.hestia.registro.controller.RelatorioMaturidadeController;
import br.com.hestia.indicadores.maturidade.*;
import br.com.hestia.usuario.model.Usuario;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.http.ResponseEntity;
import java.util.Map;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
class RelatorioMaturidadeTest {
 @Test void consultaECsvReutilizamComparacaoAutorizadaSemAlterarResultado(){
  var apuracoes=mock(ApuracaoMaturidadeService.class);var usuario=mock(Usuario.class);
  var esperado=Map.<String,Object>of("comparavel",false,"motivo","=Dado externo","anteriorId",1L,"atualId",2L);
  when(apuracoes.evolucao(usuario,1L,2L)).thenReturn(esperado);
  var controller=new RelatorioMaturidadeController(mock(ResultadoMaturidadeService.class),apuracoes);
  assertEquals(esperado,controller.evolucao(usuario,1L,2L,new MockHttpServletRequest("GET","/relatorios-maturidade/evolucao")));
  var csv=(ResponseEntity<?>)controller.evolucao(usuario,1L,2L,new MockHttpServletRequest("GET","/relatorios-maturidade/evolucao.csv"));
  assertTrue(csv.getBody().toString().contains("'=Dado externo"));assertTrue(csv.getBody().toString().contains("false"));
  verify(apuracoes,times(2)).evolucao(usuario,1L,2L);
 }
 @Test void csvPropagaRecusaDeEscopo(){
  var apuracoes=mock(ApuracaoMaturidadeService.class);var u=mock(Usuario.class);
  when(apuracoes.evolucao(u,1L,2L)).thenThrow(new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN));
  var controller=new RelatorioMaturidadeController(mock(ResultadoMaturidadeService.class),apuracoes);
  assertThrows(org.springframework.web.server.ResponseStatusException.class,()->controller.evolucao(u,1L,2L,new MockHttpServletRequest("GET","/relatorios-maturidade/evolucao.csv")));
 }

 @Test void csvPreservaListasEObjetosVaziosDoResultado(){
  var apuracoes=mock(ApuracaoMaturidadeService.class);var u=mock(Usuario.class);
  when(apuracoes.evolucao(u,1L,2L)).thenReturn(Map.of("registros",java.util.List.of(),"filtros",Map.of(),"comparavel",false));
  var controller=new RelatorioMaturidadeController(mock(ResultadoMaturidadeService.class),apuracoes);
  var csv=(ResponseEntity<?>)controller.evolucao(u,1L,2L,new MockHttpServletRequest("GET","/relatorios-maturidade/evolucao.csv"));
  assertTrue(csv.getBody().toString().contains("[]"));assertTrue(csv.getBody().toString().contains("{}"));assertTrue(csv.getBody().toString().contains("false"));
 }
}
