package br.com.hestia.indicadores;

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

@SpringBootTest
@Transactional
class ConfiancaIndicadoresIntegrationTest {
    private static final String PG_URL = System.getenv("HESTIA_PG_TEST_URL");
    private static final String SCHEMA = "confianca_" + UUID.randomUUID().toString().replace("-", "");
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
    }

    @AfterEach
    void limparContexto() { RequestContextHolder.resetRequestAttributes(); }


    @Autowired br.com.hestia.indicadores.service.ConsultaConfiancaIndicadoresService consulta;
    private final java.time.LocalDate hoje = java.time.LocalDate.now();

    private br.com.hestia.indicadores.dto.IndicadoresConfiancaDTO consultar(
            br.com.hestia.indicadores.service.ConsultaConfiancaIndicadoresService.Visao visao) {
        em.flush();
        return consulta.consultar(usuario, hoje.minusDays(1), hoje, visao, null);
    }
    private br.com.hestia.indicadores.dto.IndicadoresConfiancaDTO.Indicador uso(
            br.com.hestia.indicadores.dto.IndicadoresConfiancaDTO dto) {
        return dto.indicadores().stream().filter(i -> i.codigo().equals("usoResponsavel")).findFirst().orElseThrow();
    }

    @Test void deveDistinguirAusenciaDeZeroMedidoEManterGlobalIndisponivel() {
        var vazio = consultar(br.com.hestia.indicadores.service.ConsultaConfiancaIndicadoresService.Visao.PESSOAL);
        assertNull(uso(vazio).valor());
        assertNull(uso(vazio).confianca().cobertura());
        assertEquals("SEM_DADOS", uso(vazio).confianca().situacao());
        var p = politicas.criar(dto("Politica", true));
        for (int i = 0; i < 30; i++) avaliarCom(novoRegistro(), p.getId(), Set.of());
        var medido = consultar(br.com.hestia.indicadores.service.ConsultaConfiancaIndicadoresService.Visao.PESSOAL);
        assertEquals(0, uso(medido).valor().signum());
        assertEquals("CONFIAVEL", uso(medido).confianca().classificacao());
        assertEquals(30, uso(medido).confianca().amostra());
        assertNull(medido.scoreGlobal());
        assertEquals(0, medido.scoreParcial().signum());
        assertEquals(0, new java.math.BigDecimal("0.15").compareTo(medido.coberturaPesos()));
        assertEquals(7, medido.indicadores().stream().filter(i -> i.confianca().situacao().equals("FONTE_PENDENTE")).count());
    }

    @Test void deveRecalcularAoReceberNovasAvaliacoesEIdentificarParcial() {
        var p = politicas.criar(dto("Politica", true));
        var registros = new ArrayList<RegistroUsoIA>();
        for (int i = 0; i < 30; i++) registros.add(novoRegistro());
        var ausente = consultar(br.com.hestia.indicadores.service.ConsultaConfiancaIndicadoresService.Visao.PESSOAL);
        assertNull(uso(ausente).valor());
        assertEquals(0, uso(ausente).confianca().cobertura().signum());
        for (int i = 0; i < 29; i++) avaliarCom(registros.get(i), p.getId(), EnumSet.allOf(CriterioConformidade.class));
        var parcial = consultar(br.com.hestia.indicadores.service.ConsultaConfiancaIndicadoresService.Visao.PESSOAL);
        assertEquals("PARCIAL", uso(parcial).confianca().classificacao());
        assertEquals(29, uso(parcial).confianca().amostra());
        assertNull(parcial.scoreParcial());
        avaliarCom(registros.getLast(), p.getId(), EnumSet.allOf(CriterioConformidade.class));
        var completo = consultar(br.com.hestia.indicadores.service.ConsultaConfiancaIndicadoresService.Visao.PESSOAL);
        assertEquals("CONFIAVEL", uso(completo).confianca().classificacao());
        assertEquals(0, java.math.BigDecimal.valueOf(100).compareTo(uso(completo).valor()));
        assertEquals("CONFIANCA-1.0", completo.versaoConfianca());
    }

    @Test void deveReduzirCompletudePorRevisaoEPreservarHistoricoLegado() {
        var p = politicas.criar(dto("Politica", true));
        avaliarCom(novoRegistro(), p.getId(), EnumSet.of(CriterioConformidade.LGPD));
        em.persist(new AvaliacaoConformidade(novoRegistro(), p, StatusConformidade.NAO_CONFORME, Set.of(), "Legado"));
        var resultado = uso(consultar(br.com.hestia.indicadores.service.ConsultaConfiancaIndicadoresService.Visao.PESSOAL));
        assertNull(resultado.valor());
        assertEquals(2, resultado.confianca().universoObservado());
        assertEquals(0, new java.math.BigDecimal("0.5").compareTo(resultado.confianca().cobertura()));
        assertEquals(0, resultado.confianca().completude().signum());
        assertEquals(0, resultado.confianca().amostra());
    }

    @Test void deveRespeitarPessoaDepartamentoEEmpresa() {
        novoRegistro();
        var d = new Departamento(null, "Outro", null, null, empresa); em.persist(d);
        var outro = novoUsuario(empresa, d, "outra-pessoa@example.test");
        registroPara(outro, ferramenta);
        assertEquals(1, uso(consultar(br.com.hestia.indicadores.service.ConsultaConfiancaIndicadoresService.Visao.PESSOAL))
                .confianca().universoObservado());
        assertEquals(2, uso(consultar(br.com.hestia.indicadores.service.ConsultaConfiancaIndicadoresService.Visao.GERENCIAL))
                .confianca().universoObservado());
        usuario.setPerfil(PerfilUsuario.GESTOR);
        assertEquals(1, uso(consultar(br.com.hestia.indicadores.service.ConsultaConfiancaIndicadoresService.Visao.GERENCIAL))
                .confianca().universoObservado());
        usuario.setPerfil(PerfilUsuario.FUNCIONARIO);
        assertEquals(403, assertThrows(ResponseStatusException.class,
                () -> consultar(br.com.hestia.indicadores.service.ConsultaConfiancaIndicadoresService.Visao.GERENCIAL))
                .getStatusCode().value());
        var outraEmpresa = new Empresa(null, "Outra", "00000000000009", null, null); em.persist(outraEmpresa);
        var d2 = new Departamento(null, "Externo", null, null, outraEmpresa); em.persist(d2);
        var externo = novoUsuario(outraEmpresa, d2, "externo@example.test");
        em.flush();
        assertEquals(0, uso(consulta.consultar(externo, hoje.minusDays(1), hoje,
                br.com.hestia.indicadores.service.ConsultaConfiancaIndicadoresService.Visao.GERENCIAL, null))
                .confianca().universoObservado());
    }

    @Test void deveFiltrarPeriodoValidarVersaoEContexto() {
        novoRegistro();
        em.flush();
        var passado = consulta.consultar(usuario, hoje.minusYears(1), hoje.minusMonths(1),
                br.com.hestia.indicadores.service.ConsultaConfiancaIndicadoresService.Visao.PESSOAL, null);
        assertEquals(0, uso(passado).confianca().universoObservado());
        assertEquals(400, assertThrows(ResponseStatusException.class, () -> consulta.consultar(usuario, hoje, hoje.minusDays(1), null, null)).getStatusCode().value());
        assertEquals(400, assertThrows(ResponseStatusException.class, () -> consulta.consultar(usuario, null, hoje, null, null)).getStatusCode().value());
        assertEquals(400, assertThrows(ResponseStatusException.class, () -> consulta.consultar(usuario, hoje, hoje.plusDays(1), null, null)).getStatusCode().value());
        assertEquals(404, assertThrows(ResponseStatusException.class, () -> consulta.consultar(usuario, hoje, hoje, null, "inexistente")).getStatusCode().value());
        usuario.setAtivo(false);
        assertEquals(403, assertThrows(ResponseStatusException.class, () -> consulta.consultar(usuario, hoje, hoje, null, null)).getStatusCode().value());
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
