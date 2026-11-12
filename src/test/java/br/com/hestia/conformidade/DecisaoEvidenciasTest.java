package br.com.hestia.conformidade;
import br.com.hestia.conformidade.service.DecisaoEvidencias;
import br.com.hestia.conformidade.model.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class DecisaoEvidenciasTest {
 @Test void ausenciaNaoEViolacaoECriterioCriticoPrevalece(){
  var estados=new EnumMap<CriterioConformidade,DecisaoEvidencias.Estado>(CriterioConformidade.class);
  assertEquals(StatusConformidade.REQUER_REVISAO,DecisaoEvidencias.decidir(estados));
  for(var c:CriterioConformidade.values())estados.put(c,DecisaoEvidencias.Estado.ATENDIDO);
  assertEquals(StatusConformidade.CONFORME,DecisaoEvidencias.decidir(estados));
  estados.put(CriterioConformidade.FINALIDADE_ADEQUADA,DecisaoEvidencias.Estado.NAO_ATENDIDO);
  assertEquals(StatusConformidade.REQUER_REVISAO,DecisaoEvidencias.decidir(estados));
  estados.put(CriterioConformidade.LGPD,DecisaoEvidencias.Estado.NAO_ATENDIDO);
  assertEquals(StatusConformidade.NAO_CONFORME,DecisaoEvidencias.decidir(estados));
 }
 @Test void colaboradorNaoPodeRegistrarConferenciaConclusiva(){
  var usuarios=org.mockito.Mockito.mock(br.com.hestia.usuario.repository.UsuarioRepository.class);
  var autor=org.mockito.Mockito.mock(br.com.hestia.usuario.model.Usuario.class);
  org.mockito.Mockito.when(autor.getPerfil()).thenReturn(br.com.hestia.usuario.model.PerfilUsuario.FUNCIONARIO);
  org.mockito.Mockito.when(usuarios.findById(1L)).thenReturn(Optional.of(autor));
  var empresa=new br.com.hestia.autenticacao.contexto.EmpresaAtual();empresa.definir(new br.com.hestia.autenticacao.contexto.ContextoEmpresa(1L,10L));
  var jdbc=org.mockito.Mockito.mock(org.springframework.jdbc.core.JdbcTemplate.class);
  var service=new br.com.hestia.conformidade.service.RegistroEvidenciasService(jdbc,empresa,usuarios,org.mockito.Mockito.mock(br.com.hestia.aprendizagem.AcessoCapacitacao.class));
  assertThrows(org.springframework.web.server.ResponseStatusException.class,()->service.registrar(org.mockito.Mockito.mock(br.com.hestia.registro.model.RegistroUsoIA.class),List.of(new br.com.hestia.registro.dto.RegistroUsoIACriacaoDTO.Evidencia(CriterioConformidade.LGPD,DecisaoEvidencias.Estado.ATENDIDO,"Referência"))));
  org.mockito.Mockito.verifyNoInteractions(jdbc);
 }
}
