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
import br.com.hestia.microtreinamento.model.TipoErro;
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
    @MockitoBean ContextualizacaoRagService contextualizacao;
    @MockitoBean BuscaVetorialService busca;@MockitoBean GeracaoRagService geracao;
    Usuario admin,aluno,outro,gestor;Empresa empresa;
    @BeforeEach void preparar(){
        when(contextualizacao.interpretar(anyString(),anyList())).thenAnswer(i->new ContextualizacaoRagService.Interpretacao(i.getArgument(0),null));
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
    long publicar(){
        long id=a.publicar(admin,new Publicacao(1L,1,questoes()));
        a.publicar(admin,new Publicacao(8L,1,questoes()));
        a.publicar(admin,new Publicacao(11L,1,questoes()));
        return id;
    }
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
        // A trilha de Protecao de Dados inclui os cursos 1, 8 e 11.
        // A aprovacao no curso 1, isoladamente, nao conclui a trilha.
        for (long treinamentoId : List.of(8L, 11L)) {
            leituras.concluir(aluno, treinamentoId, UUID.randomUUID().toString());
            long catalogoComplementar = ((Number) a.catalogos(aluno, treinamentoId)
                    .getFirst().get("id")).longValue();
            var tentativaComplementar = a.abrir(aluno, catalogoComplementar);
            a.enviar(aluno, tentativaComplementar.id(), envio(5));
        }
        assertThat(trilhas.consultar(aluno,participacao).status()).isEqualTo("CONCLUIDA");
        Long conclusao=((Number)a.jdbc().queryForMap("SELECT id FROM conclusoes_microtreinamento WHERE usuario_id=? AND microtreinamento_id=?",aluno.getId(),1L).get("id")).longValue();
        var prova=vinculos.consultar(aluno,conclusao).getFirst();
        assertThat(prova.get("status")).isEqualTo("CONCLUIDA");
        assertThat(((Number)prova.get("usuario_id")).longValue()).isEqualTo(aluno.getId());
        assertThat(((Number)prova.get("empresa_id")).longValue()).isEqualTo(empresa.getId());
        assertThat(prova.get("concluida_em")).isNotNull();assertThat(prova.get("data_conclusao")).isNotNull();
        assertThatThrownBy(()->vinculos.consultar(outro,conclusao)).isInstanceOf(ResponseStatusException.class);
    }
    record InfraTrilha(long ferramenta,long politica,long versaoPolitica){}
    InfraTrilha infraestruturaTrilha(){
        long ferramenta=a.inserir("INSERT INTO ferramentas_ia(nome,fornecedor,descricao,tipo,finalidade_uso,url_acesso,status,nivel_risco,trata_dados_pessoais,data_cadastro,data_atualizacao,empresa_id) VALUES(?,?,?,?,?,?,?,?,?,?,?,?)","IA Teste","Teste","Teste","CHAT","Treinamento",null,"ATIVA","BAIXO",false,LocalDateTime.now(),LocalDateTime.now(),empresa.getId());
        long politica=a.inserir("INSERT INTO politicas_uso(titulo,descricao,conteudo,versao,ativa,data_criacao,data_atualizacao,empresa_id,revisao,excluida,versao_registro) VALUES(?,?,?,?,?,?,?,?,?,?,?)","Política","Teste","Conteúdo","1.0",true,LocalDateTime.now(),LocalDateTime.now(),empresa.getId(),1,false,0);
        long versao=a.inserir("INSERT INTO politica_uso_versoes(politica_uso_id,empresa_id,revisao,titulo,descricao,conteudo,versao,ativa,excluida,origem,registrada_em,autor_id) VALUES(?,?,?,?,?,?,?,?,?,?,?,?)",politica,empresa.getId(),1,"Política","Teste","Conteúdo","1.0",true,false,"TESTE",LocalDateTime.now(),admin.getId());
        return new InfraTrilha(ferramenta,politica,versao);
    }
    long ocorrenciaConfirmada(InfraTrilha i,TipoErro tipo,LocalDateTime data){
        long registro=a.inserir("INSERT INTO registros_uso_ia(usuario_id,ferramenta_id,modelo_ia,data_hora,finalidade,tokens_entrada,tokens_saida,tokens_total,custo_estimado,status_execucao,tempo_resposta_ms) VALUES(?,?,?,?,?,?,?,?,?,?,?)",aluno.getId(),i.ferramenta(),"modelo",data,"OUTRO",0,0,0,null,"SUCESSO",1);
        long avaliacao=a.inserir("INSERT INTO avaliacoes_conformidade(registro_uso_id,politica_uso_id,status,justificativa,data_avaliacao,politica_versao_id,regra_codigo,regra_versao,motivo_preservado,regra_preservada,limitacoes_preservadas) VALUES(?,?,?,?,?,?,?,?,?,?,?)",registro,i.politica(),"NAO_CONFORME","Confirmada",data,i.versaoPolitica(),"CONFORMIDADE_CRITERIOS","1.0","Teste","Teste","Teste");
        a.inserir("INSERT INTO trilha_ocorrencias_confirmadas(empresa_id,usuario_id,avaliacao_id,tipo_erro,ocorrido_em,confirmado_por,confirmado_em,justificativa) VALUES(?,?,?,?,?,?,?,?)",empresa.getId(),aluno.getId(),avaliacao,tipo.name(),data,admin.getId(),LocalDateTime.now(),"Evidência confirmada");
        return avaliacao;
    }
    void publicarAvaliacao(long treinamento){
        var tema=a.conteudo(treinamento).getTema();
        for(var t:new br.com.hestia.microtreinamento.service.MicrotreinamentoFicticioService().criarTreinamentos()){
            if(t.isAtivo()&&t.getTema()==tema){
                a.publicar(admin,new Publicacao(t.getId(),1,questoes()));
            }
        }
    }
    @Test void recorrenciaAcionaUmaTrilhaEReprocessamentoNaoDuplica(){
        publicarAvaliacao(5L);var infra=infraestruturaTrilha();var agora=LocalDateTime.now();
        ocorrenciaConfirmada(infra,TipoErro.AUSENCIA_VALIDACAO_HUMANA,agora.minusDays(3));
        ocorrenciaConfirmada(infra,TipoErro.AUSENCIA_VALIDACAO_HUMANA,agora.minusDays(2));
        motor.processar(admin,aluno.getId());assertThat(motor.ciclos(admin,aluno.getId())).isEmpty();
        ocorrenciaConfirmada(infra,TipoErro.AUSENCIA_VALIDACAO_HUMANA,agora.minusDays(1));
        motor.processar(admin,aluno.getId());
        assertThat(motor.ciclos(admin,aluno.getId())).hasSize(1);
        assertThat(a.jdbc().queryForObject("SELECT count(*) FROM trilha_evidencias WHERE usuario_id=?",Long.class,aluno.getId())).isEqualTo(3);
        motor.processar(admin,aluno.getId());
        assertThat(motor.ciclos(admin,aluno.getId())).hasSize(1);
        assertThat(a.jdbc().queryForObject("SELECT count(*) FROM trilha_evidencias WHERE usuario_id=?",Long.class,aluno.getId())).isEqualTo(3);
    }
    @Test void reincidenciaPosteriorCriaNovoCicloVinculado(){
        publicarAvaliacao(5L);var infra=infraestruturaTrilha();var agora=LocalDateTime.now();
        ocorrenciaConfirmada(infra,TipoErro.AUSENCIA_VALIDACAO_HUMANA,agora.minusDays(3));
        ocorrenciaConfirmada(infra,TipoErro.AUSENCIA_VALIDACAO_HUMANA,agora.minusDays(2));
        ocorrenciaConfirmada(infra,TipoErro.AUSENCIA_VALIDACAO_HUMANA,agora.minusDays(1));
        motor.processar(admin,aluno.getId());var primeiro=motor.ciclos(admin,aluno.getId()).getFirst();
        var conclusao=LocalDateTime.now().minusMinutes(1);
        a.jdbc().update("UPDATE participacoes_trilha SET status='CONCLUIDA',concluida_em=? WHERE id=?",conclusao,primeiro.get("participacao_id"));
        ocorrenciaConfirmada(infra,TipoErro.AUSENCIA_VALIDACAO_HUMANA,LocalDateTime.now());
        motor.processar(admin,aluno.getId());var ciclos=motor.ciclos(admin,aluno.getId());
        assertThat(ciclos).hasSize(2);
        assertThat(((Number)ciclos.getLast().get("anterior_id")).longValue()).isEqualTo(((Number)primeiro.get("id")).longValue());
        assertThat(ciclos.getLast().get("corte_aprendizagem")).isNotNull();
    }
    @Test void temaSemCatalogoRegistraPendenciaEAbreQuandoConfigurado(){
        var infra=infraestruturaTrilha();ocorrenciaConfirmada(infra,TipoErro.DADOS_SENSIVEIS,LocalDateTime.now().minusHours(1));
        motor.processar(admin,aluno.getId());
        assertThat(motor.ciclos(admin,aluno.getId())).isEmpty();
        assertThat(motor.pendencias(admin,aluno.getId())).extracting(p->p.get("tema")).contains("PROTECAO_DE_DADOS");
        publicarAvaliacao(1L);motor.processar(admin,aluno.getId());
        assertThat(motor.ciclos(admin,aluno.getId())).hasSize(1);
        assertThat(motor.pendencias(admin,aluno.getId())).extracting(p->p.get("tema")).doesNotContain("PROTECAO_DE_DADOS");
    }
    @Autowired org.springframework.jdbc.core.JdbcTemplate jdbcCatalogo;
    @Autowired br.com.hestia.microtreinamento.service.ConclusaoMicrotreinamentoService conclusoesCatalogo;
    @Autowired br.com.hestia.microtreinamento.service.RecorrenciaCapacitacaoService recorrenciaCatalogo;
    @Autowired br.com.hestia.microtreinamento.service.ConsultaEfetividadeCapacitacaoService efetividadeCatalogo;
    @Autowired br.com.hestia.microtreinamento.service.CatalogoTreinamentoService catalogoPadrao;

    @Test
    void integraConclusaoTrilhaEEfetividadeComCatalogoPersistido() throws Exception {
        var catalogoReal = new br.com.hestia.microtreinamento.service.CatalogoTreinamentoService(
                jdbcCatalogo, contexto, false);
        motor.usarCatalogoPersistido(catalogoReal);
        a.usarCatalogoPersistido(catalogoReal);
        conclusoesCatalogo.usarCatalogoPersistido(catalogoReal);
        recorrenciaCatalogo.usarCatalogoPersistido(catalogoReal);

        try {
            long publicado = 92001L;
            inserirTreinamento(publicado, empresa.getId(), "PUBLICADO");
            inserirTreinamento(92002L, empresa.getId(), "RETIRADO");
            inserirTreinamento(92003L, outro.getEmpresa().getId(), "PUBLICADO");

            var infra = infraestruturaTrilha();
            ocorrenciaConfirmada(infra, TipoErro.DADOS_SENSIVEIS, LocalDateTime.now().minusHours(1));
            motor.processar(admin, aluno.getId());

            assertThat(motor.ciclos(admin, aluno.getId())).isEmpty();
            assertThat(motor.pendencias(admin, aluno.getId()))
                    .extracting(p -> p.get("tema")).contains("PROTECAO_DE_DADOS");

            long avaliacao = a.publicar(admin, new Publicacao(publicado, 1, questoes()));
            var requisicao = RequestContextHolder.currentRequestAttributes();
            RequestContextHolder.resetRequestAttributes();
            motor.processar(admin, aluno.getId());

            assertThat(motor.ciclos(admin, aluno.getId())).hasSize(1);
            assertThat(motor.pendencias(admin, aluno.getId()))
                    .extracting(p -> p.get("tema")).doesNotContain("PROTECAO_DE_DADOS");
            assertThat(jdbcCatalogo.queryForList(
                    "SELECT treinamento_id FROM trilha_aprendizagem_requisitos WHERE treinamento_id IN (92001, 92002, 92003)",
                    Long.class)).containsExactly(publicado);

            RequestContextHolder.setRequestAttributes(requisicao);
            leituras.concluir(aluno, publicado, UUID.randomUUID().toString());
            var tentativa = a.abrir(aluno, avaliacao);
            assertThat(a.enviar(aluno, tentativa.id(), envio(5)).status()).isEqualTo("APROVADA");
            var participacao = ((Number) motor.ciclos(admin, aluno.getId()).getFirst()
                    .get("participacao_id")).longValue();
            assertThat(trilhas.consultar(aluno, participacao).status()).isEqualTo("CONCLUIDA");
            em.flush();

            Long conclusao = jdbcCatalogo.queryForObject(
                    "SELECT id FROM conclusoes_microtreinamento WHERE usuario_id=? AND microtreinamento_id=?",
                    Long.class, aluno.getId(), publicado);
            jdbcCatalogo.update("UPDATE conclusoes_microtreinamento SET data_conclusao=? WHERE id=?",
                    LocalDateTime.now().minusMinutes(1), conclusao);
            em.clear();
            jdbcCatalogo.update("UPDATE catalogo_treinamentos SET estado='RETIRADO', retirado_em=CURRENT_TIMESTAMP WHERE id=?",
                    publicado);

            RequestContextHolder.resetRequestAttributes();
            var resultado = efetividadeCatalogo.consultar(aluno, conclusao, TipoErro.DADOS_SENSIVEIS);
            assertThat(resultado.indicador().acompanhamento().microtreinamentoId()).isEqualTo(publicado);
            assertThat(resultado.trilhas()).extracting(t -> t.get("status")).containsExactly("CONCLUIDA");
            assertThatThrownBy(() -> efetividadeCatalogo.consultar(outro, conclusao, TipoErro.DADOS_SENSIVEIS))
                    .isInstanceOf(ResponseStatusException.class);
        } finally {
            motor.usarCatalogoPersistido(catalogoPadrao);
            a.usarCatalogoPersistido(catalogoPadrao);
            conclusoesCatalogo.usarCatalogoPersistido(catalogoPadrao);
            recorrenciaCatalogo.usarCatalogoPersistido(catalogoPadrao);
        }
    }

    private void inserirTreinamento(long id, long empresaId, String estado) throws Exception {
        String conteudo = "Confira as regras de proteção de dados antes de usar IA.";
        String hash = java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256")
                .digest(conteudo.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        jdbcCatalogo.update("""
                INSERT INTO catalogo_treinamentos
                (id, empresa_id, titulo, descricao, conteudo, tema, duracao_minutos,
                 fonte, autorizacao_uso, objetivo, publico, atividade, criterio_aceite,
                 questionario, conteudo_sha256, estado, criado_por, criado_em)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
                """, id, empresaId, "Proteção de dados", "Treinamento de integração", conteudo,
                "PROTECAO_DE_DADOS", 10, "Teste", "Teste", "Proteção de dados",
                "Equipe", "Atividade", "Critério", "[]", hash, estado, admin.getId());
    }

    RespostaReutilizavel resposta(){var r=new RespostaReutilizavel();r.setEmpresaId(empresa.getId());r.setUsuarioId(admin.getId());r.setPromptOriginal("Pergunta sobre política");r.setRespostaGerada("Somente ferramentas autorizadas.");r.setPerfisPermitidos(Set.of(PerfilUsuario.ADMIN,PerfilUsuario.FUNCIONARIO));return respostas.saveAndFlush(r);}
    @Test void revisaoBloqueiaExpiradasEHistoricoDeRetirada(){
        var r=resposta();assertThat(acesso.recuperar(aluno,null,r.getId(),null,false,org.springframework.data.domain.Pageable.unpaged())).hasSize(1);
        revisoes.revisar(admin,r.getId(),new RevisaoRespostaService.Acao(StatusQualidadeResposta.BLOQUEADA,"Revisão de validade",null, null));
        assertThat(acesso.recuperar(aluno,null,r.getId(),null,false,org.springframework.data.domain.Pageable.unpaged())).isEmpty();
        revisoes.revisar(admin,r.getId(),new RevisaoRespostaService.Acao(StatusQualidadeResposta.APROVADA,"Conteúdo verificado",LocalDateTime.now().plusDays(1), null));
        r.setExpiraEm(LocalDateTime.now().minusSeconds(1));respostas.saveAndFlush(r);assertThat(r.isElegivel()).isFalse();
        assertThat(acesso.recuperar(aluno,null,r.getId(),null,false,org.springframework.data.domain.Pageable.unpaged())).isEmpty();
        revisoes.revisar(admin,r.getId(),new RevisaoRespostaService.Acao(StatusQualidadeResposta.RETIRADA,"Retirada definitiva",null, null));
        assertThatThrownBy(()->revisoes.revisar(admin,r.getId(),new RevisaoRespostaService.Acao(StatusQualidadeResposta.APROVADA,"Tentar reabrir",null, null))).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(()->revisoes.detalhe(outro,r.getId())).isInstanceOf(ResponseStatusException.class);
    }
    @Test void ragNuncaEnviaContextoSemAutorizacaoERevalidaHistorico(){
        var r=resposta();when(busca.buscarSemelhantes(eq(empresa.getId()),anyString(),eq(5))).thenReturn(List.of(r));
        when(geracao.gerar(anyString(),anyList())).thenReturn(new GeracaoRagService.Gerada("Resposta baseada no trecho","modelo-teste",10L,5L));
        var p=new InteracaoRagService.Pergunta("gerar-1","Qual ferramenta usar?",InteracaoRagService.Modo.GERAR_NOVA,null);
        assertThat(rag.perguntar(aluno,p).fontes()).hasSize(1);rag.perguntar(aluno,p);
        verify(geracao,times(1)).gerar(eq(p.pergunta()),argThat(t->t.size()==1&&t.getFirst().contains("Somente ferramentas autorizadas.")));
        revisoes.revisar(admin,r.getId(),new RevisaoRespostaService.Acao(StatusQualidadeResposta.RETIRADA,"Fim da validade",null, null));
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
        assertThat(consulta.confianca()).allMatch(c->c.classificacao().equals("DADOS_INSUFICIENTES"));
        Long x=apuracoes.registrar(admin,primeira),y=apuracoes.registrar(admin,segunda);
        var preservada=apuracoes.obter(admin,x);assertThat((String)preservada.get("conteudo")).contains("proposta-0.1","scoreGlobal");
        assertThat(apuracoes.evolucao(admin,x,y).get("comparavel")).isEqualTo(true);
        assertThat(apuracoes.evolucao(admin,x,y).get("variacaoGlobal")).isNull();
        assertThatThrownBy(()->apuracoes.obter(outro,x)).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(()->apuracoes.obter(gestor,x)).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(()->apuracoes.obter(aluno,x)).isInstanceOf(ResponseStatusException.class);
    }

    private long movimentosReuso(Usuario u){return a.jdbc().queryForObject("SELECT count(*) FROM movimentos_experiencia WHERE usuario_id=? AND acao='REUTILIZACAO_RESPOSTA'",Long.class,u.getId());}
    @Test void consultaNaoPremiaEConfirmacaoRepetidaContaUmaVez(){
        var fonte=resposta();fonte.setTokensEntrada(100);fonte.setTokensSaida(20);respostas.saveAndFlush(fonte);
        var consulta=rag.perguntar(aluno,new InteracaoRagService.Pergunta("consulta-544",fonte.getPromptOriginal(),InteracaoRagService.Modo.CONSULTAR,null));
        assertThat(consulta.estado()).isEqualTo("RECUPERADA");assertThat(consulta.natureza()).isEqualTo("CONSULTA");
        assertThat(consulta.similaridade()).isNull();assertThat(consulta.confianca()).isEqualTo("NAO_CLASSIFICADA");
        assertThat(consulta.fontes().getFirst().id()).isEqualTo(fonte.getId());
        assertThat(fonte.getQuantidadeReutilizacoes()).isZero();assertThat(movimentosReuso(aluno)).isZero();
        var pedido=new InteracaoRagService.Pergunta("confirmar-544",fonte.getPromptOriginal(),InteracaoRagService.Modo.REUTILIZAR,fonte.getId());
        var confirmada=rag.perguntar(aluno,pedido);assertThat(confirmada.estado()).isEqualTo("REUTILIZADA");
        assertThat(rag.perguntar(aluno,pedido).id()).isEqualTo(confirmada.id());
        em.flush();assertThat(fonte.getQuantidadeReutilizacoes()).isEqualTo(1);assertThat(fonte.getTokensEconomizados()).isEqualTo(120);
        assertThat(movimentosReuso(aluno)).isEqualTo(1);verifyNoInteractions(geracao);
    }
    @Test void mesmaChaveNaoPodeConfirmarOutraResposta(){
        var primeira=resposta();var segunda=resposta();
        rag.perguntar(aluno,new InteracaoRagService.Pergunta("mesma-chave-544","Escolha",InteracaoRagService.Modo.REUTILIZAR,primeira.getId()));
        assertThatThrownBy(()->rag.perguntar(aluno,new InteracaoRagService.Pergunta("mesma-chave-544","Escolha",InteracaoRagService.Modo.REUTILIZAR,segunda.getId())))
                .isInstanceOfSatisfying(ResponseStatusException.class,e->assertThat(e.getStatusCode().value()).isEqualTo(409));
        assertThat(segunda.getQuantidadeReutilizacoes()).isZero();
    }
    @Test void respostaRetiradaNaoPodeSerConfirmadaDepoisDaConsulta(){
        var fonte=resposta();rag.perguntar(aluno,new InteracaoRagService.Pergunta("ver-544",fonte.getPromptOriginal(),InteracaoRagService.Modo.CONSULTAR,null));
        revisoes.revisar(admin,fonte.getId(),new RevisaoRespostaService.Acao(StatusQualidadeResposta.RETIRADA,"Conteúdo retirado",null, null));
        assertThatThrownBy(()->rag.perguntar(aluno,new InteracaoRagService.Pergunta("retirada-544","Escolha",InteracaoRagService.Modo.REUTILIZAR,fonte.getId())))
                .isInstanceOf(ResponseStatusException.class);
        assertThat(fonte.getQuantidadeReutilizacoes()).isZero();assertThat(movimentosReuso(aluno)).isZero();
    }
    @Test void confirmacaoNaoAcessaRespostaDeOutraEmpresa(){
        var fonte=resposta();
        assertThatThrownBy(()->rag.perguntar(outro,new InteracaoRagService.Pergunta("outra-empresa-544","Escolha",InteracaoRagService.Modo.REUTILIZAR,fonte.getId())))
                .isInstanceOf(ResponseStatusException.class);
        assertThat(fonte.getQuantidadeReutilizacoes()).isZero();assertThat(movimentosReuso(outro)).isZero();
    }
    @Test void reenviosConcorrentesEUsuariosDistintosNaoDuplicamNemPerdemContadores() throws Exception {
        var fonte=resposta();fonte.setTokensEntrada(100);fonte.setTokensSaida(20);respostas.saveAndFlush(fonte);
        long respostaId=fonte.getId(),empresaId=empresa.getId();
        TestTransaction.flagForCommit();TestTransaction.end();
        var executor=java.util.concurrent.Executors.newFixedThreadPool(6);
        var largada=new java.util.concurrent.CountDownLatch(1);
        try{
            var futuros=new ArrayList<java.util.concurrent.Future<Long>>();
            for(int i=0;i<6;i++){Usuario autor=i%2==0?aluno:admin;
                futuros.add(executor.submit(()->{
                    RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));
                    contexto.definir(new ContextoEmpresa(autor.getId(),empresaId));
                    try{largada.await();return rag.perguntar(autor,new InteracaoRagService.Pergunta("concorrente-544","Escolha confirmada",InteracaoRagService.Modo.REUTILIZAR,respostaId)).id();}
                    finally{RequestContextHolder.resetRequestAttributes();}
                }));
            }
            largada.countDown();var ids=new HashSet<Long>();
            for(var futuro:futuros)ids.add(futuro.get(30,java.util.concurrent.TimeUnit.SECONDS));
            assertThat(ids).hasSize(2);
            assertThat(a.jdbc().queryForObject("SELECT quantidade_reutilizacoes FROM respostas_reutilizaveis WHERE id=?",Integer.class,respostaId)).isEqualTo(2);
            assertThat(a.jdbc().queryForObject("SELECT tokens_economizados FROM respostas_reutilizaveis WHERE id=?",Long.class,respostaId)).isEqualTo(240);
            assertThat(movimentosReuso(aluno)).isEqualTo(1);assertThat(movimentosReuso(admin)).isEqualTo(1);
        }finally{executor.shutdownNow();}
    }

    @Autowired br.com.hestia.reutilizavel.controller.RespostaReutilizavelController controllerReuso;
    @Test void endpointLegadoExigeChaveECompartilhaConfirmacaoIdempotente() throws Exception {
        var fonte=resposta();
        var mvc=org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup(controllerReuso)
                .addFilters((jakarta.servlet.Filter)(req,res,chain)->{
                    contexto.definir(new ContextoEmpresa(aluno.getId(),empresa.getId()));
                    chain.doFilter(req,res);
                }).build();
        String url="/api/respostas-reutilizaveis/"+fonte.getId()+"/reutilizar";
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch(url).requestAttr("usuarioAutenticado",aluno))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isBadRequest());
        assertThat(fonte.getQuantidadeReutilizacoes()).isZero();
        for(int i=0;i<2;i++)mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch(url)
                        .header("Idempotency-Key","legado-544").requestAttr("usuarioAutenticado",aluno))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk());
        em.flush();assertThat(fonte.getQuantidadeReutilizacoes()).isEqualTo(1);assertThat(movimentosReuso(aluno)).isEqualTo(1);
    }

    @Test void continuacaoReformulaBuscaEEnviaHistoricoComFontesAtuaisAoModelo(){
        var fonte=resposta();var conversa=rag.criarConversa(aluno,"Proteção de dados");
        when(busca.buscarSemelhantes(eq(empresa.getId()),anyString(),eq(5))).thenReturn(List.of(fonte));
        when(geracao.gerar(anyString(),anyList())).thenReturn(new GeracaoRagService.Gerada("Não envie dados pessoais.","teste",10L,5L));
        String primeira="Posso enviar dados pessoais para IA?",atual="E se forem anonimizados?",consulta="Posso enviar dados anonimizados para uma ferramenta de IA?";
        rag.perguntar(aluno,new InteracaoRagService.Pergunta("contexto-1",primeira,InteracaoRagService.Modo.GERAR_NOVA,null,conversa.id()));
        when(contextualizacao.interpretar(eq(atual),anyList())).thenReturn(new ContextualizacaoRagService.Interpretacao(consulta,null));
        var resultado=rag.perguntar(aluno,new InteracaoRagService.Pergunta("contexto-2",atual,InteracaoRagService.Modo.GERAR_NOVA,null,conversa.id()));
        assertThat(resultado.pergunta()).isEqualTo(atual);assertThat(resultado.estado()).isEqualTo("GERADA_COM_FONTES");
        verify(busca).buscarSemelhantes(empresa.getId(),consulta,5);
        verify(contextualizacao).interpretar(eq(atual),argThat(h->h.size()==1&&h.getFirst().pergunta().equals(primeira)&&h.getFirst().resposta().contains("Não envie")));
        verify(geracao).gerar(argThat(q->q.contains(primeira)&&q.contains(atual)&&q.contains(consulta)&&q.contains("não é fonte de regras")),argThat(f->f.size()==1&&f.getFirst().contains("Somente ferramentas autorizadas")));
        assertThat(rag.historicoConversa(aluno,conversa.id())).hasSize(2);
    }
    @Test void ambiguidadePersistePedidoDeEsclarecimentoSemBuscaGeracaoOuCredito(){
        var conversa=rag.criarConversa(aluno,"Sem assunto");
        when(contextualizacao.interpretar(eq("E isso?"),anyList())).thenReturn(new ContextualizacaoRagService.Interpretacao(null,"A qual dado você se refere?"));
        var p=new InteracaoRagService.Pergunta("ambiguo-546","E isso?",InteracaoRagService.Modo.GERAR_NOVA,null,conversa.id());
        var r=rag.perguntar(aluno,p);assertThat(r.estado()).isEqualTo("PRECISA_ESCLARECIMENTO");assertThat(r.resposta()).contains("qual dado");assertThat(r.fontes()).isEmpty();
        assertThat(rag.perguntar(aluno,p).id()).isEqualTo(r.id());assertThat(movimentosReuso(aluno)).isZero();verifyNoInteractions(busca,geracao);
    }
    @Test void fonteRevogadaNaoVaiParaInterpretacaoDaContinuacao(){
        var fonte=resposta();var conversa=rag.criarConversa(aluno,"Política");
        when(busca.buscarSemelhantes(eq(empresa.getId()),anyString(),eq(5))).thenReturn(List.of(fonte));
        rag.perguntar(aluno,new InteracaoRagService.Pergunta("antes-546",fonte.getPromptOriginal(),InteracaoRagService.Modo.CONSULTAR,null,conversa.id()));
        revisoes.revisar(admin,fonte.getId(),new RevisaoRespostaService.Acao(StatusQualidadeResposta.RETIRADA,"Retirada",null, null));
        when(contextualizacao.interpretar(eq("E isso?"),anyList())).thenReturn(new ContextualizacaoRagService.Interpretacao(null,"Pode detalhar a pergunta?"));
        assertThat(rag.perguntar(aluno,new InteracaoRagService.Pergunta("depois-546","E isso?",InteracaoRagService.Modo.CONSULTAR,null,conversa.id())).estado()).isEqualTo("PRECISA_ESCLARECIMENTO");
        verify(contextualizacao).interpretar(eq("E isso?"),argThat(List::isEmpty));verifyNoInteractions(geracao);
    }
    @Test void novaConversaNaoHerdaHistoricoDaAnterior(){
        var fonte=resposta();var uma=rag.criarConversa(aluno,"Uma");var outra=rag.criarConversa(aluno,"Outra");
        rag.perguntar(aluno,new InteracaoRagService.Pergunta("privada-546",fonte.getPromptOriginal(),InteracaoRagService.Modo.CONSULTAR,null,uma.id()));
        rag.perguntar(aluno,new InteracaoRagService.Pergunta("nova-546",fonte.getPromptOriginal(),InteracaoRagService.Modo.CONSULTAR,null,outra.id()));
        verify(contextualizacao,times(2)).interpretar(eq(fonte.getPromptOriginal()),argThat(List::isEmpty));
    }

    @Test void historicoRevalidaFontesRevogadasSemMarcarTransacaoComoFalha(){
        var fonte=resposta();var conversa=rag.criarConversa(aluno,"Revogação real");
        rag.perguntar(aluno,new InteracaoRagService.Pergunta("revogacao-http-546",fonte.getPromptOriginal(),InteracaoRagService.Modo.CONSULTAR,null,conversa.id()));
        TestTransaction.flagForCommit();TestTransaction.end();
        revisoes.revisar(admin,fonte.getId(),new RevisaoRespostaService.Acao(StatusQualidadeResposta.RETIRADA,"Retirada de validação",null, null));
        var historico=rag.historicoConversa(aluno,conversa.id());
        assertThat(historico).hasSize(1);assertThat(historico.getFirst().estado()).isEqualTo("FONTES_INDISPONIVEIS");
        assertThat(historico.getFirst().resposta()).isNull();assertThat(historico.getFirst().fontes()).isEmpty();
    }
}