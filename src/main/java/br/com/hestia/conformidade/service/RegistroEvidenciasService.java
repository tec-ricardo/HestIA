package br.com.hestia.conformidade.service;
import org.springframework.stereotype.Service;
import org.springframework.jdbc.core.JdbcTemplate;
import br.com.hestia.aprendizagem.AcessoCapacitacao;
import br.com.hestia.autenticacao.contexto.EmpresaAtual;
import br.com.hestia.usuario.repository.UsuarioRepository;
import br.com.hestia.usuario.model.PerfilUsuario;
import br.com.hestia.registro.model.RegistroUsoIA;
import br.com.hestia.registro.dto.RegistroUsoIACriacaoDTO.Evidencia;
import br.com.hestia.conformidade.model.CriterioConformidade;
import java.util.*;
import static br.com.hestia.aprendizagem.AcessoCapacitacao.exigir;
import org.springframework.http.HttpStatus;
@Service
public class RegistroEvidenciasService {
 private final JdbcTemplate jdbc;private final EmpresaAtual empresa;private final UsuarioRepository usuarios;private final AcessoCapacitacao acesso;
 public RegistroEvidenciasService(JdbcTemplate jdbc,EmpresaAtual empresa,UsuarioRepository usuarios,AcessoCapacitacao acesso){this.jdbc=jdbc;this.empresa=empresa;this.usuarios=usuarios;this.acesso=acesso;}
 public void registrar(RegistroUsoIA uso,List<Evidencia> evidencias){
  if(evidencias==null||evidencias.isEmpty())return;
  var autor=usuarios.findById(empresa.obterContexto().usuarioId()).orElseThrow();
  exigir(autor.getPerfil()!=PerfilUsuario.FUNCIONARIO,HttpStatus.FORBIDDEN,"Evidências conclusivas exigem conferência do gestor ou administrador");
  acesso.usuario(autor,uso.getUsuario().getId());
  var permitidos=EnumSet.of(CriterioConformidade.FINALIDADE_ADEQUADA,CriterioConformidade.DADOS_SENSIVEIS,CriterioConformidade.VALIDACAO_HUMANA,CriterioConformidade.LGPD);
  var vistos=EnumSet.noneOf(CriterioConformidade.class);
  exigir(evidencias.size()<=4,HttpStatus.BAD_REQUEST,"No máximo quatro critérios conferidos");
  for(var e:evidencias){
   exigir(e!=null&&e.criterio()!=null&&permitidos.contains(e.criterio())&&vistos.add(e.criterio())&&e.estado()!=null&&e.referencia()!=null&&!e.referencia().isBlank()&&e.referencia().length()<=2000,HttpStatus.BAD_REQUEST,"Critério, estado e referência minimizada obrigatórios; não duplique critérios");
   jdbc.update("INSERT INTO evidencias_criterios_uso VALUES(?,?,?,?,?,?)",uso.getId(),e.criterio().name(),e.estado().name(),e.referencia(),autor.getId(),java.time.LocalDateTime.now());
  }
 }
}
