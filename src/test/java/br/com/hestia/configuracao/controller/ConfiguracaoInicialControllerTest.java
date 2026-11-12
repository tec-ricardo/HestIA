package br.com.hestia.configuracao.controller;
import br.com.hestia.autenticacao.contexto.*;
import br.com.hestia.configuracao.repository.ConfiguracaoEmpresaRepository;
import br.com.hestia.configuracao.service.ConfiguracaoEmpresaService;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class ConfiguracaoInicialControllerTest {
 @Test void rejeitaOutraEmpresaAntesDeConsultarOuSalvar() {
  var empresa = new EmpresaAtual(); empresa.definir(new ContextoEmpresa(1L, 10L));
  var jdbc = mock(JdbcTemplate.class); var repo = mock(ConfiguracaoEmpresaRepository.class); var service = mock(ConfiguracaoEmpresaService.class);
  var controller = new ConfiguracaoInicialController(empresa, jdbc, repo, service);
  assertThrows(IllegalArgumentException.class, () -> controller.consultar(20L));
  assertThrows(IllegalArgumentException.class, () -> controller.salvar(20L, null));
  verifyNoInteractions(jdbc, repo, service);
 }
 @Test void consultaNaoCriaCadastrosNemDeclaraCatalogoReal() {
  var empresa = new EmpresaAtual(); empresa.definir(new ContextoEmpresa(1L, 10L));
  var jdbc = mock(JdbcTemplate.class); var repo = mock(ConfiguracaoEmpresaRepository.class); var service = mock(ConfiguracaoEmpresaService.class);
  when(jdbc.queryForObject(anyString(), eq(Long.class), eq(10L))).thenReturn(0L);
  when(jdbc.queryForObject(anyString(), eq(Long.class), eq(10L), anyString())).thenReturn(0L);
  var resultado = new ConfiguracaoInicialController(empresa, jdbc, repo, service).consultar(10L);
  assertNull(resultado.configuracao());
  assertEquals(8, resultado.etapas().stream().filter(e -> e.estado().equals("PENDENTE")).count());
  assertTrue(resultado.etapas().stream().anyMatch(e -> e.nome().contains("catálogo") && e.estado().equals("PENDENTE")));
  verify(repo, never()).save(any()); verifyNoInteractions(service);
 }
}
