package br.com.hestia.politica;

import br.com.hestia.autenticacao.contexto.*;
import br.com.hestia.conformidade.dto.AvaliacaoConformidadeDTO;
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
class PoliticaHistoricoIntegrationTest {
    private static final String PG_URL = System.getenv("HESTIA_PG_TEST_URL");
    private static final String SCHEMA = "politicas_" + UUID.randomUUID().toString().replace("-", "");
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

    @Test
    void deveManterResultadoETextoOriginaisAposAtualizarEExcluirPolitica() {
        Long politicaId = politicas.criar(dto("Conteudo original", true)).getId();
        var primeira = avaliar(novoRegistro(), politicaId);
        assertEquals(StatusConformidade.CONFORME, primeira.status());
        assertEquals(1, primeira.politicaRevisao());
        assertNotNull(primeira.politicaVersaoId());
        assertTrue(primeira.fundamentosPreservados());
        var dataOriginal = primeira.dataAvaliacao();

        politicas.atualizar(politicaId, dto("Conteudo atualizado", false));
        var segunda = avaliar(novoRegistro(), politicaId);
        assertEquals(StatusConformidade.NAO_CONFORME, segunda.status());
        assertEquals(2, segunda.politicaRevisao());
        assertNotEquals(primeira.politicaVersaoId(), segunda.politicaVersaoId());
        // Expulsa as entidades do cache para provar a preservacao no banco.
        em.flush(); em.clear();

        var consultada = avaliacoes.buscar(primeira.id());
        assertEquals(primeira.motivo(), consultada.motivo());
        assertEquals(primeira.regraAplicada(), consultada.regraAplicada());
        assertEquals(primeira.limitacoes(), consultada.limitacoes());
        assertTrue(Math.abs(java.time.Duration.between(dataOriginal, consultada.dataAvaliacao()).toNanos()) < 1000);
        assertEquals(StatusConformidade.CONFORME, consultada.status());
        assertEquals("Conteudo original", avaliacoes.buscarPoliticaHistorica(primeira.id()).conteudo());
        assertEquals("Conteudo atualizado", avaliacoes.buscarPoliticaHistorica(segunda.id()).conteudo());

        politicas.excluir(politicaId);
        politicas.excluir(politicaId);
        em.flush(); em.clear();
        assertTrue(politicas.listarTodas().isEmpty());
        var historico = politicas.listarVersoes(politicaId);
        assertEquals(List.of(3, 2, 1), historico.stream().map(v -> v.revisao()).toList());
        assertTrue(historico.getFirst().excluida());
        assertEquals(usuario.getId(), historico.getFirst().autorId());
        assertEquals("Conteudo original", politicas.buscarVersao(politicaId, 1).conteudo());
        assertEquals("Conteudo original", avaliacoes.buscarPoliticaHistorica(primeira.id()).conteudo());
    }

    @Test
    void deveBloquearNovaAvaliacaoComPoliticaExcluida() {
        Long id = politicas.criar(dto("Conteudo", true)).getId();
        var registro = novoRegistro();
        politicas.excluir(id);
        assertThrows(IllegalArgumentException.class, () -> avaliar(registro, id));
        assertEquals(0L, em.createQuery("select count(a) from AvaliacaoConformidade a", Long.class)
                .getSingleResult());
    }

    @Test
    void deveIsolarHistoricoEAvaliacaoPorEmpresa() {
        Long id = politicas.criar(dto("Conteudo reservado", true)).getId();
        var a = avaliar(novoRegistro(), id);
        contexto.definir(new ContextoEmpresa(usuario.getId(), empresa.getId() + 9999L));
        assertThrows(IllegalArgumentException.class, () -> politicas.listarVersoes(id));
        assertThrows(IllegalArgumentException.class, () -> politicas.buscarVersao(id, 1));
        assertThrows(IllegalArgumentException.class, () -> avaliacoes.buscar(a.id()));
        assertThrows(IllegalArgumentException.class, () -> avaliacoes.buscarPoliticaHistorica(a.id()));
    }

    @Test
    void naoDeveFabricarFundamentosParaAvaliacaoLegada() {
        var politica = politicas.criar(dto("Politica atual", true));
        var legado = new AvaliacaoConformidade(novoRegistro(), politica,
                StatusConformidade.CONFORME, EnumSet.allOf(CriterioConformidade.class), "Legada");
        em.persist(legado); em.flush();
        Long id = legado.getId();
        em.clear();
        var resposta = avaliacoes.buscar(id);
        assertEquals(StatusConformidade.CONFORME, resposta.status());
        assertFalse(resposta.fundamentosPreservados());
        assertNull(resposta.politicaVersaoId());
        assertNull(resposta.regraVersao());
        assertNull(resposta.motivo());
        assertThrows(NoSuchElementException.class, () -> avaliacoes.buscarPoliticaHistorica(id));
    }

    private br.com.hestia.conformidade.dto.AvaliacaoConformidadeResponseDTO avaliar(
            RegistroUsoIA registro, Long politicaId) {
        return avaliacoes.avaliar(new AvaliacaoConformidadeDTO(registro.getId(), politicaId,
                EnumSet.of(CriterioConformidade.DADOS_SENSIVEIS,
                        CriterioConformidade.VALIDACAO_HUMANA, CriterioConformidade.LGPD), "Revisado"));
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
