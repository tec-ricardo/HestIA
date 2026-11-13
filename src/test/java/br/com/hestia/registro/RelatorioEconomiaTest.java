package br.com.hestia.registro;
import br.com.hestia.registro.controller.RelatorioEconomiaController;
import br.com.hestia.reutilizavel.service.RespostaReutilizavelService;
import br.com.hestia.reutilizavel.dto.EconomiaReutilizacaoDTO;
import br.com.hestia.usuario.model.*;
import br.com.hestia.empresa.model.Empresa;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.http.ResponseEntity;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class RelatorioEconomiaTest {
 @Test void exportaMesmoAcumuladoSemInventarPreco(){
  var s=mock(RespostaReutilizavelService.class);var u=mock(Usuario.class);var empresa=mock(Empresa.class);
  when(u.getPerfil()).thenReturn(PerfilUsuario.ADMIN);when(u.getEmpresa()).thenReturn(empresa);when(empresa.getId()).thenReturn(10L);
  when(s.consultarEconomia(10L)).thenReturn(new EconomiaReutilizacaoDTO(10L,5,100,null,50));
  var c=new RelatorioEconomiaController(s);
  var dados=(Map<?,?>)c.consultar(u,Map.of(),new MockHttpServletRequest("GET","/relatorios-economia"));assertNull(dados.get("custoEstimadoEvitado"));
  var csv=(ResponseEntity<?>)c.consultar(u,Map.of(),new MockHttpServletRequest("GET","/relatorios-economia/exportar"));
  assertTrue(csv.getBody().toString().contains("\"custoEstimadoEvitado\",\r\n"));verify(s,times(2)).consultarEconomia(10L);
  assertThrows(org.springframework.web.server.ResponseStatusException.class,()->c.consultar(u,Map.of("inicio","2026-11-01"),new MockHttpServletRequest("GET","/relatorios-economia")));
 }
 @Test void gestorNaoExportaAcumuladoEmpresarial(){var s=mock(RespostaReutilizavelService.class);var u=mock(Usuario.class);when(u.getPerfil()).thenReturn(PerfilUsuario.GESTOR);assertThrows(org.springframework.web.server.ResponseStatusException.class,()->new RelatorioEconomiaController(s).consultar(u,Map.of(),new MockHttpServletRequest("GET","/relatorios-economia/exportar")));verifyNoInteractions(s);}
}
