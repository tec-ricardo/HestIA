package br.com.hestia.usuario.service;
import org.junit.jupiter.api.Test;
import br.com.hestia.usuario.model.*;
import br.com.hestia.usuario.dto.UsuarioDTO;
import br.com.hestia.usuario.repository.UsuarioRepository;
import br.com.hestia.empresa.model.Empresa;
import br.com.hestia.empresa.repository.EmpresaRepository;
import br.com.hestia.departamento.model.Departamento;
import br.com.hestia.departamento.repository.DepartamentoRepository;
import br.com.hestia.autenticacao.repository.SessaoUsuarioRepository;
import br.com.hestia.autenticacao.contexto.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.Optional;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
class RevogacaoAcessoTest {
 @Test void revogaAoMudarPerfilMasNaoAoEditarNome() {
  var ur=mock(UsuarioRepository.class);var er=mock(EmpresaRepository.class);var dr=mock(DepartamentoRepository.class);var sr=mock(SessaoUsuarioRepository.class);
  var e=mock(Empresa.class);when(e.getId()).thenReturn(10L);var d=mock(Departamento.class);when(d.getId()).thenReturn(2L);when(d.getEmpresa()).thenReturn(e);
  var u=new Usuario();u.setEmpresa(e);u.setDepartamento(d);u.setPerfil(PerfilUsuario.ADMIN);u.setAtivo(true);
  when(ur.findById(1L)).thenReturn(Optional.of(u));when(er.findById(10L)).thenReturn(Optional.of(e));when(dr.findById(2L)).thenReturn(Optional.of(d));when(ur.save(u)).thenReturn(u);
  var ctx=new EmpresaAtual();ctx.definir(new ContextoEmpresa(9L,10L));
  var service=new UsuarioService(ur,er,dr,mock(PasswordEncoder.class),null,sr,ctx);
  var dto=new UsuarioDTO();dto.setEmpresaId(10L);dto.setDepartamentoId(2L);dto.setPerfil(PerfilUsuario.ADMIN);dto.setNome("Nome atualizado");
  service.atualizar(1L,dto);verifyNoInteractions(sr);
  dto.setPerfil(PerfilUsuario.GESTOR);service.atualizar(1L,dto);verify(sr).revogarTodas(eq(1L),any());
  dto.setEmpresaId(20L);assertThrows(IllegalArgumentException.class,()->service.atualizar(1L,dto));
 }
}
