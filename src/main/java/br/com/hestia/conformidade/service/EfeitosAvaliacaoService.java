package br.com.hestia.conformidade.service;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.*;
import org.springframework.jdbc.core.JdbcTemplate;
import br.com.hestia.conformidade.repository.*;
import br.com.hestia.conformidade.model.*;
import br.com.hestia.gamificacao.service.GamificacaoService;
import br.com.hestia.gamificacao.dto.RegistroAcaoDTO;
import br.com.hestia.gamificacao.model.AcaoExperiencia;
import br.com.hestia.trilha.service.MotorTrilhasService.ConformidadeRegistrada;
@Service
public class EfeitosAvaliacaoService {
 private final JdbcTemplate jdbc;private final AvaliacaoConformidadeRepository avaliacoes;private final TratamentoNaoConformidadeRepository tratamentos;private final GamificacaoService gamificacao;
 public EfeitosAvaliacaoService(JdbcTemplate jdbc,AvaliacaoConformidadeRepository avaliacoes,TratamentoNaoConformidadeRepository tratamentos,GamificacaoService gamificacao){this.jdbc=jdbc;this.avaliacoes=avaliacoes;this.tratamentos=tratamentos;this.gamificacao=gamificacao;}
 @TransactionalEventListener(phase=TransactionPhase.BEFORE_COMMIT)
 public void aplicar(ConformidadeRegistrada evento){
  if(evento.avaliacaoId()==null)return;
  var a=avaliacoes.buscarBloqueada(evento.avaliacaoId()).orElseThrow();var u=a.getRegistroUso().getUsuario();
  if(!"2.0".equals(a.getRegraVersao())&&!"2.1".equals(a.getRegraVersao()))return;
  if(!u.getId().equals(evento.usuarioId())||!u.getEmpresa().getId().equals(evento.empresaId()))throw new IllegalArgumentException("Origem de avaliação incompatível");
  jdbc.queryForObject("SELECT id FROM usuarios WHERE id=? FOR UPDATE",Long.class,u.getId());
  if(a.getStatus()==StatusConformidade.CONFORME)gamificacao.registrarAcao(new RegistroAcaoDTO(u.getId(),AcaoExperiencia.USO_CONFORME,"AVALIACAO_CONFORME:"+a.getId()));
  if(a.getStatus()==StatusConformidade.NAO_CONFORME&&!tratamentos.existsByAvaliacao_Id(a.getId()))tratamentos.save(new TratamentoNaoConformidade(a,u));
 }
}
