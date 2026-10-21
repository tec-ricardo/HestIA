package br.com.hestia.validacao;

import br.com.hestia.autenticacao.contexto.*;
import br.com.hestia.conformidade.dto.*;
import org.springframework.web.server.ResponseStatusException;
import br.com.hestia.conformidade.model.*;
import br.com.hestia.conformidade.service.AvaliacaoConformidadeService;
import br.com.hestia.departamento.model.Departamento;
import br.com.hestia.empresa.model.Empresa;
import br.com.hestia.ferramenta.model.*;
import br.com.hestia.politica.dto.PoliticaUsoDTO;
import br.com.hestia.politica.service.PoliticaUsoService;
import br.com.hestia.registro.model.*;
import br.com.hestia.usuario.model.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.*;
import java.sql.DriverManager;
import java.time.LocalDateTime;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Transactional
class RecorrenciaCenariosConhecidosIntegrationTest {
    private static final String PG_URL = System.getenv("HESTIA_PG_TEST_URL");
    private static final String SCHEMA = "cenarios_" + UUID.randomUUID().toString().replace("-", "");
    @Autowired EntityManager em;
    @Autowired PoliticaUsoService politicas;
    @Autowired AvaliacaoConformidadeService avaliacoes;
    @Autowired EmpresaAtual contexto;
    private Empresa empresa;
    private Usuario usuario;
    private FerramentaIA ferramenta;

    @DynamicPropertySource
    static void configurar(DynamicPropertyRegistry r) throws Exception {
        // A mesma prova de comportamento roda em H2 no CI e em PostgreSQL quando configurado.
        if (PG_URL == null || PG_URL.isBlank()) return;
        try (var c = conectar(); var s = c.createStatement()) {
            s.execute("CREATE SCHEMA " + SCHEMA);
        }
        r.add("spring.datasource.url", () -> PG_URL + (PG_URL.contains("?") ? "&" : "?")
                + "currentSchema=" + SCHEMA + ",public");
        r.add("spring.datasource.username", () -> System.getenv("HESTIA_PG_TEST_USER"));
        r.add("spring.datasource.password", () -> System.getenv("HESTIA_PG_TEST_PASSWORD"));
        r.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        r.add("spring.flyway.enabled", () -> "true");
        r.add("spring.flyway.schemas", () -> SCHEMA);
        r.add("spring.flyway.default-schema", () -> SCHEMA);
        r.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    }

    private static java.sql.Connection conectar() throws Exception {
        return DriverManager.getConnection(PG_URL, System.getenv("HESTIA_PG_TEST_USER"),
                System.getenv("HESTIA_PG_TEST_PASSWORD"));
    }

    @AfterAll
    static void removerSchema() throws Exception {
        if (PG_URL == null || PG_URL.isBlank()) return;
        try (var c = conectar(); var s = c.createStatement()) {
            s.execute("DROP SCHEMA IF EXISTS " + SCHEMA + " CASCADE");
        }
    }

    @BeforeEach
    void preparar() {
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));
        empresa = new Empresa(null, "Empresa de teste", "00000000000003", null, null);
        em.persist(empresa);
        var departamento = new Departamento(null, "Teste", null, null, empresa);
        em.persist(departamento);
        usuario = new Usuario();
        usuario.setNome("Usuario de teste"); usuario.setEmail("historico@example.test");
        usuario.setSenha("credencial-exclusiva-do-teste"); usuario.setCargo("Teste");
        usuario.setPerfil(PerfilUsuario.ADMIN); usuario.setEmpresa(empresa);
        usuario.setDepartamento(departamento);
        em.persist(usuario);
        ferramenta = new FerramentaIA("Ferramenta de teste", "Fornecedor de teste", null,
                TipoFerramentaIA.CHATBOT, "Teste", null, false, empresa);
        ReflectionTestUtils.setField(ferramenta, "status", StatusFerramentaIA.APROVADA);
        em.persist(ferramenta);
        contexto.definir(new ContextoEmpresa(usuario.getId(), empresa.getId()));
        em.flush();
        mvc=org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup(web).build();
    }

    @AfterEach
    void limparContexto() { RequestContextHolder.resetRequestAttributes(); }


    @Autowired br.com.hestia.microtreinamento.service.ConsultaEfetividadeCapacitacaoService consulta;
    @Autowired br.com.hestia.microtreinamento.service.FonteRecorrenciaConformidade fonte;
    @Autowired br.com.hestia.autenticacao.service.AutenticacaoService autenticacao;
    @Autowired org.springframework.security.crypto.password.PasswordEncoder encoder;
    @Autowired org.springframework.web.context.WebApplicationContext web;
    // Auditoria usa REQUIRES_NEW e não enxerga as contas ainda não commitadas das fixtures.
    // A retirada documentada é validada nos próprios campos persistidos, na transação de teste.
    @org.springframework.test.context.bean.override.mockito.MockitoBean
    br.com.hestia.auditoria.service.AuditoriaService auditoria;
    private org.springframework.test.web.servlet.MockMvc mvc;
    private final LocalDateTime marco = LocalDateTime.now().minusDays(45).withNano(0);

    private br.com.hestia.microtreinamento.model.ConclusaoMicrotreinamento conclusao(LocalDateTime data, long curso) {
        var c = new br.com.hestia.microtreinamento.model.ConclusaoMicrotreinamento(usuario.getId(), curso, data, true);
        em.persist(c); em.flush(); return c;
    }
    private void registro(LocalDateTime data, boolean erro, boolean avaliar, Long politica) {
        ReflectionTestUtils.setField(ferramenta, "status", erro ? StatusFerramentaIA.BLOQUEADA : StatusFerramentaIA.APROVADA);
        var r = new RegistroUsoIA(usuario, ferramenta, "Modelo do cenário", data,
                FinalidadeUso.PESQUISA, 1, 1, null, StatusExecucao.SUCESSO, 1L);
        em.persist(r); em.flush();
        if (avaliar) {
            var a = avaliarCom(r, politica, EnumSet.allOf(CriterioConformidade.class));
            // Datas históricas explícitas das fixtures: não modificam dados operacionais.
            ReflectionTestUtils.setField(em.find(AvaliacaoConformidade.class, a.id()), "dataAvaliacao", data);
            em.flush();
        }
    }
    private Long politica() { return politicas.criar(dto("Política do cenário controlado", true)).getId(); }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.CsvSource({
        "4,1,MELHORIA,75.00", "3,3,ESTABILIDADE,0.00", "2,3,PIORA,-50.00",
        "3,0,MELHORIA,100.00", "3,2,MELHORIA,33.33"
    })
    void deveCalcularCenariosConhecidosAPartirDoBanco(int antes, int depois, String situacao, String percentual) {
        var p = politica(); var c = conclusao(marco, 2);
        for (int i=0; i<antes; i++) registro(marco.minusDays(1).plusMinutes(i), true, true, p);
        for (int i=0; i<depois; i++) registro(marco.plusDays(1).plusMinutes(i), true, true, p);
        registro(marco.plusDays(2), false, true, p); // Evidência posterior real, inclusive quando não há erro.
        var r = consulta.consultar(usuario, c.getId(), br.com.hestia.microtreinamento.model.TipoErro.FERRAMENTA_NAO_AUTORIZADA);
        assertTrue(r.cobertura().confirmada());
        assertEquals(situacao, r.indicador().situacao().name());
        assertEquals(new java.math.BigDecimal(percentual), r.indicador().reducaoPercentual());
        var rec = r.indicador().acompanhamento().recorrencia();
        assertEquals(antes, rec.ocorrenciasAntes()); assertEquals(depois, rec.ocorrenciasDepois());
        assertEquals(antes >= 3, rec.recorrenteAntes()); assertEquals(depois >= 3, rec.recorrenteDepois());
        assertTrue(rec.periodoPosteriorCompleto());
        assertTrue(r.cobertura().limitacao().contains("causal"));
    }

    @Test void zeroPosteriorSemAvaliacaoCompletaNaoPodeVirarMelhoria() {
        var p=politica(); var c=conclusao(marco,2);
        for(int i=0;i<3;i++) registro(marco.minusDays(1).plusMinutes(i),true,true,p);
        registro(marco.plusDays(1),false,true,p);
        registro(marco.plusDays(2),false,false,p);
        var r=consulta.consultar(usuario,c.getId(),br.com.hestia.microtreinamento.model.TipoErro.FERRAMENTA_NAO_AUTORIZADA);
        assertFalse(r.cobertura().confirmada());
        assertEquals(0,r.indicador().acompanhamento().recorrencia().ocorrenciasDepois());
        assertEquals("DADOS_INSUFICIENTES",r.indicador().situacao().name());
        assertEquals("COBERTURA_NAO_CONFIRMADA",r.indicador().motivo().name());
        assertNull(r.indicador().reducaoPercentual());
    }

    @Test void deveDistinguirSemRegistrosDeSemErrosComRegistrosAvaliados() {
        var p=politica(); var c=conclusao(marco,2);
        var vazio=consulta.consultar(usuario,c.getId(),br.com.hestia.microtreinamento.model.TipoErro.FERRAMENTA_NAO_AUTORIZADA);
        assertFalse(vazio.cobertura().confirmada()); assertEquals(0,vazio.cobertura().antes().registros());
        assertNull(vazio.indicador().reducaoPercentual());
        registro(marco.minusDays(1),false,true,p); registro(marco.plusDays(1),false,true,p);
        var observado=consulta.consultar(usuario,c.getId(),br.com.hestia.microtreinamento.model.TipoErro.FERRAMENTA_NAO_AUTORIZADA);
        assertTrue(observado.cobertura().confirmada());
        assertEquals("SEM_OCORRENCIAS_ANTERIORES",observado.indicador().motivo().name());
        assertNull(observado.indicador().reducaoPercentual());
    }

    @Test void periodoAindaAbertoETipoSemMapeamentoNaoDevemProduzirPercentualDefinitivo() {
        var p=politica(); var recente=LocalDateTime.now().minusDays(5).withNano(0);
        var c=conclusao(recente,2);
        registro(recente.minusDays(1),true,true,p); registro(recente.plusDays(1),false,true,p);
        var aberto=consulta.consultar(usuario,c.getId(),br.com.hestia.microtreinamento.model.TipoErro.FERRAMENTA_NAO_AUTORIZADA);
        assertEquals("PERIODO_POSTERIOR_INCOMPLETO",aberto.indicador().motivo().name());
        assertNull(aberto.indicador().reducaoPercentual());
        var protecao=conclusao(marco,1);
        var naoSuportado=consulta.consultar(usuario,protecao.getId(),br.com.hestia.microtreinamento.model.TipoErro.DADOS_SENSIVEIS);
        assertFalse(naoSuportado.cobertura().tipoSuportado());
        assertNull(naoSuportado.indicador().reducaoPercentual());
    }

    @Test void deveRespeitarLimitesTemporaisNoBancoSemDuplicarAMudancaDeJanela() {
        var p=politica();
        registro(marco.minusDays(30).minusSeconds(1),true,true,p);
        registro(marco.minusDays(30),true,true,p);
        registro(marco.minusSeconds(1),true,true,p);
        registro(marco,true,true,p);
        registro(marco.plusDays(5),true,true,p);
        registro(marco.plusDays(6),true,true,p);
        registro(marco.plusDays(30),true,true,p);
        var leitura=fonte.consultar(empresa.getId(),usuario.getId(),
                br.com.hestia.microtreinamento.model.TipoErro.FERRAMENTA_NAO_AUTORIZADA,marco,marco.plusDays(5));
        assertEquals(2,leitura.antes().registros()); assertEquals(2,leitura.depois().registros());
        assertEquals(4,leitura.ocorrencias().size());
        assertEquals(4,leitura.ocorrencias().stream().map(br.com.hestia.microtreinamento.dto.OcorrenciaErroDTO::origemId).distinct().count());
    }

    @Test void conclusaoDeOutraEmpresaOuPessoaNaoDeveSerExpostaPelaApi() throws Exception {
        var c=conclusao(marco,2);
        var outra=new Empresa(null,"Outra","00000000000009",null,null); em.persist(outra);
        var d=new Departamento(null,"Outra",null,null,outra); em.persist(d);
        var externo=novoUsuario(outra,d,"externo@example.test");
        externo.setSenha(encoder.encode("somente-teste")); em.flush();
        String externoToken=autenticacao.autenticar(new br.com.hestia.autenticacao.dto.LoginDTO(externo.getEmail(),"somente-teste")).token();
        mvc.perform(get("/microtreinamentos/conclusoes/"+c.getId()+"/efetividade")
                .param("tipoErro","FERRAMENTA_NAO_AUTORIZADA").header("Authorization","Bearer "+externoToken))
                .andExpect(status().isNotFound());
        var outro=novoUsuario(empresa,usuario.getDepartamento(),"outro@example.test");
        outro.setPerfil(PerfilUsuario.FUNCIONARIO); outro.setSenha(encoder.encode("somente-teste")); em.flush();
        String token=autenticacao.autenticar(new br.com.hestia.autenticacao.dto.LoginDTO(outro.getEmail(),"somente-teste")).token();
        mvc.perform(get("/microtreinamentos/conclusoes/"+c.getId()+"/efetividade")
                .param("tipoErro","FERRAMENTA_NAO_AUTORIZADA").header("Authorization","Bearer "+token))
                .andExpect(status().isNotFound());
        mvc.perform(get("/microtreinamentos/conclusoes/"+c.getId()+"/efetividade")
                .param("tipoErro","FERRAMENTA_NAO_AUTORIZADA")).andExpect(status().isUnauthorized());
    }
    private Usuario novoUsuario(Empresa e, Departamento d, String email) {
        var u = new Usuario();
        u.setNome("Usuario de teste"); u.setEmail(email); u.setSenha("somente-teste");
        u.setCargo("Teste"); u.setPerfil(PerfilUsuario.ADMIN); u.setEmpresa(e); u.setDepartamento(d);
        em.persist(u);
        return u;
    }

    private RegistroUsoIA registroPara(Usuario u, FerramentaIA f) {
        var r = new RegistroUsoIA(u, f, "Teste", LocalDateTime.now(), FinalidadeUso.PESQUISA,
                1, 1, null, StatusExecucao.SUCESSO, 1L);
        em.persist(r); em.flush();
        return r;
    }

    private br.com.hestia.conformidade.dto.AvaliacaoConformidadeResponseDTO avaliarCom(
            RegistroUsoIA r, Long politicaId, Set<CriterioConformidade> criterios) {
        return avaliacoes.avaliar(new AvaliacaoConformidadeDTO(r.getId(), politicaId, criterios, "Verificado"));
    }
    private RegistroUsoIA novoRegistro() {
        var r = new RegistroUsoIA(em.getReference(Usuario.class, usuario.getId()),
                em.getReference(FerramentaIA.class, ferramenta.getId()), "Modelo de teste",
                LocalDateTime.now(), FinalidadeUso.PESQUISA, 10, 10, null, StatusExecucao.SUCESSO, 20L);
        em.persist(r); em.flush();
        return r;
    }

    private PoliticaUsoDTO dto(String conteudo, boolean ativa) {
        var d = new PoliticaUsoDTO();
        d.setTitulo("Politica de teste"); d.setDescricao("Descricao da politica");
        d.setConteudo(conteudo); d.setVersao("1.0"); d.setEmpresaId(empresa.getId()); d.setAtiva(ativa);
        return d;
    }
}
