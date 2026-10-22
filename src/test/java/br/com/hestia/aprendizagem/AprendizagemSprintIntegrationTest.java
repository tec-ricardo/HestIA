package br.com.hestia.aprendizagem;
import br.com.hestia.usuario.model.*;
import br.com.hestia.empresa.model.Empresa;
import br.com.hestia.departamento.model.Departamento;
import br.com.hestia.reutilizavel.model.*;
import br.com.hestia.reutilizavel.repository.RespostaReutilizavelRepository;
import br.com.hestia.reutilizavel.service.*;
import br.com.hestia.rag.service.*;
import br.com.hestia.autenticacao.contexto.*;
import br.com.hestia.trilha.service.*;
import br.com.hestia.ferramenta.model.*;
import br.com.hestia.registro.dto.*;
import br.com.hestia.registro.model.*;
import br.com.hestia.registro.service.RegistroUsoIAService;
import jakarta.persistence.EntityManager;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.web.context.request.*;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.server.ResponseStatusException;
import org.junit.jupiter.api.*;
import java.util.*;
import java.time.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static br.com.hestia.aprendizagem.ContratoAvaliacao.*;

@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:sprint_restante;MODE=PostgreSQL;DB_CLOSE_DELAY=-1","spring.jpa.hibernate.ddl-auto=validate","spring.flyway.enabled=true"})
@Transactional
class AprendizagemSprintIntegrationTest {
    static final String PG_URL=System.getenv("HESTIA_SPRINT_PG_TEST_URL");
    static final String SCHEMA="sprint_"+UUID.randomUUID().toString().replace("-","");
    @org.springframework.test.context.DynamicPropertySource
    static void banco(org.springframework.test.context.DynamicPropertyRegistry r) throws Exception {
        if(PG_URL==null||PG_URL.isBlank())return;
        try(var c=java.sql.DriverManager.getConnection(PG_URL,System.getenv("HESTIA_PG_TEST_USER"),System.getenv("HESTIA_PG_TEST_PASSWORD"));var s=c.createStatement()){s.execute("CREATE SCHEMA "+SCHEMA);}
        r.add("spring.datasource.url",()->PG_URL+(PG_URL.contains("?")?"&":"?")+"currentSchema="+SCHEMA+",public");
        r.add("spring.datasource.username",()->System.getenv("HESTIA_PG_TEST_USER"));r.add("spring.datasource.password",()->System.getenv("HESTIA_PG_TEST_PASSWORD"));r.add("spring.datasource.driver-class-name",()->"org.postgresql.Driver");r.add("spring.flyway.schemas",()->SCHEMA);r.add("spring.flyway.default-schema",()->SCHEMA);
    }
    @AfterAll static void limparBanco() throws Exception {
        if(PG_URL!=null&&!PG_URL.isBlank())try(var c=java.sql.DriverManager.getConnection(PG_URL,System.getenv("HESTIA_PG_TEST_USER"),System.getenv("HESTIA_PG_TEST_PASSWORD"));var s=c.createStatement()){s.execute("DROP SCHEMA IF EXISTS "+SCHEMA+" CASCADE");}
    }
    @Autowired EntityManager em;@Autowired AvaliacaoAprendizagemService a;@Autowired LeituraConteudoService leituras;
    @Autowired CertificacaoService certificados;@Autowired MotorTrilhasService motor;@Autowired TrilhaService trilhas;
    @Autowired RevisaoRespostaService revisoes;@Autowired RespostaReutilizavelRepository respostas;@Autowired AcessoRespostaService acesso;
    @Autowired InteracaoRagService rag;@Autowired EmpresaAtual contexto;@Autowired RegistroUsoIAService registros;
    @Autowired br.com.hestia.indicadores.maturidade.ApuracaoMaturidadeService apuracoes;
    @Autowired br.com.hestia.microtreinamento.service.VinculosTrilhaConclusaoPort vinculos;
    @MockitoBean BuscaVetorialService busca;@MockitoBean GeracaoRagService geracao;
    Usuario admin,aluno,outro,gestor;Empresa empresa;
    @BeforeEach void preparar(){
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));
        empresa=new Empresa(null,"Teste",UUID.randomUUID().toString().replace("-","").substring(0,14),null,null);em.persist(empresa);
        var outra=new Empresa(null,"Outra",UUID.randomUUID().toString().replace("-","").substring(0,14),null,null);em.persist(outra);
        var d=new Departamento(null,"Área",null,null,empresa);em.persist(d);var e=new Departamento(null,"Outra",null,null,outra);em.persist(e);
        admin=usuario(empresa,d,PerfilUsuario.ADMIN);aluno=usuario(empresa,d,PerfilUsuario.FUNCIONARIO);gestor=usuario(empresa,d,PerfilUsuario.GESTOR);outro=usuario(outra,e,PerfilUsuario.ADMIN);
        em.flush();contexto.definir(new ContextoEmpresa(aluno.getId(),empresa.getId()));
    }
    @AfterEach void limpar(){RequestContextHolder.resetRequestAttributes();}
    Usuario usuario(Empresa e,Departamento d,PerfilUsuario p){var u=new Usuario();u.setNome("Teste");u.setEmail(UUID.randomUUID()+"@example.test");u.setSenha("hash");u.setCargo("Teste");u.setEmpresa(e);u.setDepartamento(d);u.setPerfil(p);em.persist(u);return u;}
    List<Questao> questoes(){return java.util.stream.IntStream.rangeClosed(1,5).mapToObj(i->new Questao("q"+i,"Situação "+i,new LinkedHashMap<>(Map.of("A","Conduta autorizada","B","Conduta incorreta","C","Outra conduta")),"A","Confira o conteúdo e os critérios de uso responsável.")).toList();}
    long publicar(){return a.publicar(admin,new Publicacao(1L,1,questoes()));}
    Envio envio(int acertos){return new Envio(java.util.stream.IntStream.rangeClosed(1,5).mapToObj(i->new Resposta("q"+i,i<=acertos?"A":"B")).toList());}
    Tentativa abrir(long id){leituras.concluir(aluno,1L,UUID.randomUUID().toString());return a.abrir(aluno,id);}
    @Test void corrigeNoServidorPreservaVersaoEReenvioNaoDuplica(){
        long id=publicar();assertThatThrownBy(()->a.abrir(aluno,id)).isInstanceOf(ResponseStatusException.class);
        var t=abrir(id);assertThat(t.feedback()).isEmpty();assertThat(t.nota()).isNull();
        var aprovada=a.enviar(aluno,t.id(),envio(4));assertThat(aprovada.nota()).isEqualTo(80);assertThat(aprovada.feedback()).hasSize(5);
        assertThat(a.enviar(aluno,t.id(),envio(0)).nota()).isEqualTo(80);
        assertThat(a.historico(aluno,aluno.getId())).hasSize(1);
        assertThatThrownBy(()->a.abrir(aluno,id)).isInstanceOf(ResponseStatusException.class);
        long nova=a.publicar(admin,new Publicacao(1L,2,questoes()));assertThat(a.consultar(aluno,t.id()).versao()).isEqualTo(1);assertThat(nova).isNotEqualTo(id);
    }
    @Test void envioInvalidoNaoFinalizaNemConsomeTentativa(){
        var t=abrir(publicar());var incompleto=new Envio(List.of(new Resposta("q1","A")));
        assertThatThrownBy(()->a.enviar(aluno,t.id(),incompleto)).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(()->a.enviar(aluno,t.id(),new Envio(Collections.nCopies(5,new Resposta("q1","A"))))).isInstanceOf(ResponseStatusException.class);
        assertThat(a.consultar(aluno,t.id()).status()).isEqualTo("EM_ANDAMENTO");
        assertThat(a.abrir(aluno,a.catalogos(aluno,1L).stream().map(r->((Number)r.get("id")).longValue()).findFirst().orElseThrow()).id()).isEqualTo(t.id());
    }
    @Test void protegeEmpresaUsuarioEPermissaoDePublicacao(){
        assertThatThrownBy(()->a.publicar(aluno,new Publicacao(1L,1,questoes()))).isInstanceOf(ResponseStatusException.class);
        long id=publicar();var t=abrir(id);
        assertThatThrownBy(()->a.consultar(outro,t.id())).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(()->a.enviar(admin,t.id(),envio(5))).isInstanceOf(ResponseStatusException.class);
        aluno.setAtivo(false);em.flush();assertThatThrownBy(()->a.consultar(aluno,t.id())).isInstanceOf(ResponseStatusException.class);
    }
    @Test void tresReprovacoesExigemReforcoELiberacaoEspecifica(){
        long id=publicar();leituras.concluir(aluno,1L,"leitura-inicial");Tentativa t=null;
        for(int i=0;i<3;i++){t=a.abrir(aluno,id);a.enviar(aluno,t.id(),envio(3));}
        assertThatThrownBy(()->a.abrir(aluno,id)).isInstanceOf(ResponseStatusException.class);
        Long ciclo=t.cicloId();var d=new Liberacao("liberar-1","Reforço supervisionado",List.of(leituras.concluir(aluno,1L,"reforco-1")));
        assertThatThrownBy(()->a.liberar(gestor,ciclo,d)).isInstanceOf(ResponseStatusException.class);
        Long novo=a.liberar(admin,ciclo,d);assertThat(a.liberar(admin,ciclo,d)).isEqualTo(novo);
        assertThat(a.abrir(aluno,id).numero()).isEqualTo(1);assertThat(a.historico(aluno,aluno.getId())).hasSize(4);
    }
    @Test void certificacaoExigeConclusaoEAprovacaoEConcedeUmaUnicaVez(){
        long catalogo=publicar();certificados.publicar(admin,new CertificacaoService.Publicacao("DADOS","Proteção de dados","Inicial",1,List.of(catalogo)));
        var pendente=certificados.verificar(aluno,aluno.getId()).getFirst();
        assertThat(pendente.concedida()).isFalse();assertThat(pendente.nivel()).isEqualTo("Inicial");
        assertThat(pendente.requisitosAtendidos()).isZero();assertThat(pendente.totalRequisitos()).isEqualTo(1);
        assertThat(pendente.avaliacoesPendentes()).containsExactly(catalogo);
        var t=abrir(catalogo);a.enviar(aluno,t.id(),envio(4));
        TestTransaction.flagForCommit();TestTransaction.end();
        assertThat(certificados.historico(aluno,aluno.getId())).hasSize(1);
        var concedida=certificados.verificar(aluno,aluno.getId()).getFirst();
        assertThat(concedida.concedida()).isTrue();assertThat(concedida.requisitosAtendidos()).isEqualTo(1);
        assertThat(concedida.avaliacoesPendentes()).isEmpty();assertThat(certificados.historico(aluno,aluno.getId())).hasSize(1);
    }
    @Test void reprovacaoAcionaUmCicloEAprendizagemConcluiSemApagarHistorico(){
        long catalogo=publicar();var t=abrir(catalogo);a.enviar(aluno,t.id(),envio(3));
        TestTransaction.flagForCommit();TestTransaction.end();
        assertThat(motor.ciclos(aluno,aluno.getId())).hasSize(1);motor.processar(aluno,aluno.getId());assertThat(motor.ciclos(aluno,aluno.getId())).hasSize(1);
        Long participacao=((Number)motor.ciclos(aluno,aluno.getId()).getFirst().get("participacao_id")).longValue();
        assertThat(trilhas.consultar(aluno,participacao).status()).isNotEqualTo("CONCLUIDA");
        var segunda=a.abrir(aluno,catalogo);a.enviar(aluno,segunda.id(),envio(5));
        assertThat(trilhas.consultar(aluno,participacao).status()).isEqualTo("CONCLUIDA");
        Long conclusao=((Number)a.jdbc().queryForMap("SELECT id FROM conclusoes_microtreinamento WHERE usuario_id=? AND microtreinamento_id=?",aluno.getId(),1L).get("id")).longValue();
        var prova=vinculos.consultar(aluno,conclusao).getFirst();
        assertThat(prova.get("status")).isEqualTo("CONCLUIDA");
        assertThat(((Number)prova.get("usuario_id")).longValue()).isEqualTo(aluno.getId());
        assertThat(((Number)prova.get("empresa_id")).longValue()).isEqualTo(empresa.getId());
        assertThat(prova.get("concluida_em")).isNotNull();assertThat(prova.get("data_conclusao")).isNotNull();
        assertThatThrownBy(()->vinculos.consultar(outro,conclusao)).isInstanceOf(ResponseStatusException.class);
    }
    RespostaReutilizavel resposta(){var r=new RespostaReutilizavel();r.setEmpresaId(empresa.getId());r.setUsuarioId(admin.getId());r.setPromptOriginal("Pergunta sobre política");r.setRespostaGerada("Somente ferramentas autorizadas.");r.setPerfisPermitidos(Set.of(PerfilUsuario.ADMIN,PerfilUsuario.FUNCIONARIO));return respostas.saveAndFlush(r);}
    @Test void revisaoBloqueiaExpiradasEHistoricoDeRetirada(){
        var r=resposta();assertThat(acesso.recuperar(aluno,null,r.getId(),null,false,org.springframework.data.domain.Pageable.unpaged())).hasSize(1);
        revisoes.revisar(admin,r.getId(),new RevisaoRespostaService.Acao(StatusQualidadeResposta.BLOQUEADA,"Revisão de validade",null));
        assertThat(acesso.recuperar(aluno,null,r.getId(),null,false,org.springframework.data.domain.Pageable.unpaged())).isEmpty();
        revisoes.revisar(admin,r.getId(),new RevisaoRespostaService.Acao(StatusQualidadeResposta.APROVADA,"Conteúdo verificado",LocalDateTime.now().plusDays(1)));
        r.setExpiraEm(LocalDateTime.now().minusSeconds(1));respostas.saveAndFlush(r);assertThat(r.isElegivel()).isFalse();
        assertThat(acesso.recuperar(aluno,null,r.getId(),null,false,org.springframework.data.domain.Pageable.unpaged())).isEmpty();
        revisoes.revisar(admin,r.getId(),new RevisaoRespostaService.Acao(StatusQualidadeResposta.RETIRADA,"Retirada definitiva",null));
        assertThatThrownBy(()->revisoes.revisar(admin,r.getId(),new RevisaoRespostaService.Acao(StatusQualidadeResposta.APROVADA,"Tentar reabrir",null))).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(()->revisoes.detalhe(outro,r.getId())).isInstanceOf(ResponseStatusException.class);
    }
    @Test void ragNuncaEnviaContextoSemAutorizacaoERevalidaHistorico(){
        var r=resposta();when(busca.buscarSemelhantes(eq(empresa.getId()),anyString(),eq(5))).thenReturn(List.of(r));
        when(geracao.gerar(anyString(),anyList())).thenReturn(new GeracaoRagService.Gerada("Resposta baseada no trecho","modelo-teste",10L,5L));
        var p=new InteracaoRagService.Pergunta("gerar-1","Qual ferramenta usar?",InteracaoRagService.Modo.GERAR_NOVA,null);
        assertThat(rag.perguntar(aluno,p).fontes()).hasSize(1);rag.perguntar(aluno,p);
        verify(geracao,times(1)).gerar(eq(p.pergunta()),argThat(t->t.size()==1&&t.getFirst().contains("Somente ferramentas autorizadas.")));
        revisoes.revisar(admin,r.getId(),new RevisaoRespostaService.Acao(StatusQualidadeResposta.RETIRADA,"Fim da validade",null));
        assertThat(rag.historico(aluno).getFirst().resposta()).isNull();
        assertThat(rag.historico(outro)).isEmpty();
    }
    @Test void ragSemFonteNaoChamaGeracao(){
        when(busca.buscarSemelhantes(eq(empresa.getId()),anyString(),eq(5))).thenReturn(List.of());
        var r=rag.perguntar(aluno,new InteracaoRagService.Pergunta("vazio","Pergunta",InteracaoRagService.Modo.GERAR_NOVA,null));
        assertThat(r.estado()).isEqualTo("SEM_CONTEUDO_AUTORIZADO");assertThat(r.resposta()).isNull();verifyNoInteractions(geracao);
    }
    @Test void origemManualECapturadaSaoDistintasSemInventarMedidas(){
        var ferramenta=new FerramentaIA("IA","Fornecedor","Descrição",TipoFerramentaIA.CHATBOT,"Apoio",null,false,empresa);org.springframework.test.util.ReflectionTestUtils.setField(ferramenta,"status",StatusFerramentaIA.APROVADA);em.persist(ferramenta);em.flush();
        var dto=new RegistroUsoIACriacaoDTO(aluno.getId(),ferramenta.getId(),"modelo",LocalDateTime.now(),FinalidadeUso.PESQUISA,10,5,null,StatusExecucao.SUCESSO,null);
        assertThat(registros.criar(dto).origem()).isEqualTo(OrigemRegistro.MANUAL);
        var capturado=registros.criarCapturado(dto,"evento-1");assertThat(capturado.origem()).isEqualTo(OrigemRegistro.PLATAFORMA);assertThat(registros.criarCapturado(dto,"evento-1").id()).isEqualTo(capturado.id());
        assertThat(capturado.custoEstimado()).isNull();
    }
    @Test void apuracaoPreservaRegrasAusenciaEEscopoSemInventarConfianca(){
        var primeira=new br.com.hestia.indicadores.maturidade.ResultadoMaturidadeService.Consulta(LocalDate.of(2026,1,1),LocalDate.of(2026,1,31),br.com.hestia.indicadores.maturidade.ResultadoMaturidadeService.Natureza.OPERACIONAL,null,null,null,null);
        var segunda=new br.com.hestia.indicadores.maturidade.ResultadoMaturidadeService.Consulta(LocalDate.of(2026,3,1),LocalDate.of(2026,3,31),primeira.fonte(),null,null,null,null);
        var consulta=apuracoes.consultar(admin,primeira);assertThat(consulta.resultado().scoreGlobal()).isNull();
        assertThat(consulta.confianca()).allMatch(c->c.classificacao().equals("NAO_CLASSIFICADA"));
        Long x=apuracoes.registrar(admin,primeira),y=apuracoes.registrar(admin,segunda);
        var preservada=apuracoes.obter(admin,x);assertThat((String)preservada.get("conteudo")).contains("proposta-0.1","scoreGlobal");
        assertThat(apuracoes.evolucao(admin,x,y).get("comparavel")).isEqualTo(true);
        assertThat(apuracoes.evolucao(admin,x,y).get("variacaoGlobal")).isNull();
        assertThatThrownBy(()->apuracoes.obter(outro,x)).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(()->apuracoes.obter(gestor,x)).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(()->apuracoes.obter(aluno,x)).isInstanceOf(ResponseStatusException.class);
    }
}
