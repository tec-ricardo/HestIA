package br.com.hestia.conformidade;

import br.com.hestia.autenticacao.contexto.*;
import br.com.hestia.conformidade.dto.*;
import br.com.hestia.conformidade.service.TratamentoNaoConformidadeService;
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
class RevisaoTratamentoIntegrationTest {
    private static final String PG_URL = System.getenv("HESTIA_PG_TEST_URL");
    private static final String SCHEMA = "revisao_" + UUID.randomUUID().toString().replace("-", "");
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


    @Autowired TratamentoNaoConformidadeService tratamentos;
    @Autowired br.com.hestia.indicadores.service.ConsultaConfiancaIndicadoresService indicadores;

    private TratamentoDetalheDTO abrir() {
        var a = avaliarCom(novoRegistro(), politicas.criar(dto("Política original", true)).getId(), Set.of());
        var t = tratamentos.abrir(new AberturaTratamentoDTO(a.id()));
        em.flush(); em.clear();
        return tratamentos.buscar(t.tratamento().id());
    }

    @Test void devePreservarOriginalEHistoricoCompletoComAutorDataESequencia() {
        var original = abrir();
        Long id = original.tratamento().id();
        var confirmado = tratamentos.revisar(id, new RevisaoTratamentoDTO(
                DecisaoRevisao.CONFIRMAR, null, "  Evidências conferidas  ", original.tratamento().versaoRegistro()));
        var atualizado = tratamentos.atualizar(id, new AtualizacaoTratamentoDTO(usuario.getId(),
                "Corrigir procedimento", StatusTratamento.EM_TRATAMENTO, confirmado.tratamento().versaoRegistro()));
        var revisado = tratamentos.revisar(id, new RevisaoTratamentoDTO(DecisaoRevisao.REVISAR,
                StatusConformidade.CONFORME, "Evidência demonstra autorização na ocorrência",
                atualizado.tratamento().versaoRegistro()));
        var confirmadoNovamente = tratamentos.revisar(id, new RevisaoTratamentoDTO(
                DecisaoRevisao.CONFIRMAR, null, "Revisão conferida", revisado.tratamento().versaoRegistro()));
        em.flush(); em.clear();
        var leitura = tratamentos.buscar(id);
        assertEquals(original.avaliacao(), leitura.avaliacao());
        assertEquals(original.ocorrencia(), leitura.ocorrencia());
        assertEquals(original.politicaAplicada(), leitura.politicaAplicada());
        assertEquals(original.tratamento().criadoPorId(), leitura.tratamento().criadoPorId());
        assertEquals(original.tratamento().criadoEm(), leitura.tratamento().criadoEm());
        assertEquals(StatusConformidade.NAO_CONFORME, leitura.tratamento().resultadoOriginal());
        assertEquals(StatusConformidade.CONFORME, leitura.tratamento().resultadoVigente());
        assertEquals(StatusTratamento.EM_TRATAMENTO, leitura.tratamento().status());
        assertEquals(3, leitura.tratamento().revisoesAvaliacao());
        assertEquals(confirmadoNovamente.tratamento().versaoRegistro(), leitura.tratamento().versaoRegistro());

        var historico = tratamentos.historico(id, 0, 20);
        assertEquals(5, historico.totalItens());
        assertEquals(List.of(TipoEventoTratamento.CONFIRMACAO, TipoEventoTratamento.REVISAO,
                TipoEventoTratamento.ATUALIZACAO, TipoEventoTratamento.CONFIRMACAO, TipoEventoTratamento.ABERTURA),
                historico.itens().stream().map(EventoTratamentoDTO::tipo).toList());
        assertEquals(List.of(4L, 3L, 2L, 1L, 0L), historico.itens().stream().map(EventoTratamentoDTO::sequencia).toList());
        historico.itens().forEach(e -> { assertEquals(usuario.getId(), e.autorId()); assertNotNull(e.registradoEm()); });
        assertEquals("Evidências conferidas", historico.itens().get(3).justificativa());
        assertEquals(StatusConformidade.NAO_CONFORME, historico.itens().get(1).resultadoAnterior());
        assertEquals(StatusConformidade.CONFORME, historico.itens().get(1).resultadoVigente());
        assertEquals(StatusTratamento.ABERTA, historico.itens().getLast().statusTratamento());
        assertNull(historico.itens().getLast().acaoCorretiva());
        assertEquals("Corrigir procedimento", historico.itens().get(2).acaoCorretiva());
    }

    @Test void deveRejeitarJustificativaInvalidaDecisaoAmbiguaEVersaoAntigaSemCriarEventos() {
        var t = abrir().tratamento();
        for (String justificativa : Arrays.asList(null, "", "   ", "x".repeat(1001))) {
            assertThrows(IllegalArgumentException.class, () -> tratamentos.revisar(t.id(),
                    new RevisaoTratamentoDTO(DecisaoRevisao.CONFIRMAR, null, justificativa, t.versaoRegistro())));
        }
        assertThrows(IllegalArgumentException.class, () -> tratamentos.revisar(t.id(),
                new RevisaoTratamentoDTO(null, null, "Justificativa", t.versaoRegistro())));
        assertThrows(IllegalArgumentException.class, () -> tratamentos.revisar(t.id(),
                new RevisaoTratamentoDTO(DecisaoRevisao.CONFIRMAR, StatusConformidade.CONFORME, "Justificativa", t.versaoRegistro())));
        for (StatusConformidade resultado : Arrays.asList(null, StatusConformidade.NAO_CONFORME)) {
            assertThrows(IllegalArgumentException.class, () -> tratamentos.revisar(t.id(),
                    new RevisaoTratamentoDTO(DecisaoRevisao.REVISAR, resultado, "Justificativa", t.versaoRegistro())));
        }
        assertEquals(1, tratamentos.historico(t.id(), 0, 20).totalItens());
        var confirmado = tratamentos.revisar(t.id(), new RevisaoTratamentoDTO(
                DecisaoRevisao.CONFIRMAR, null, "Confirmado", t.versaoRegistro()));
        assertThrows(IllegalArgumentException.class, () -> tratamentos.revisar(t.id(),
                new RevisaoTratamentoDTO(DecisaoRevisao.REVISAR, StatusConformidade.CONFORME, "Tentativa obsoleta", t.versaoRegistro())));
        assertThrows(IllegalArgumentException.class, () -> tratamentos.atualizar(t.id(),
                new AtualizacaoTratamentoDTO(null, null, StatusTratamento.ABERTA, t.versaoRegistro())));
        assertEquals(2, tratamentos.historico(t.id(), 0, 20).totalItens());
        assertEquals(confirmado.tratamento().versaoRegistro(), tratamentos.buscar(t.id()).tratamento().versaoRegistro());
    }

    private void negarId(Long id, Long avaliacaoId) {
        assertThrows(NoSuchElementException.class, () -> tratamentos.buscar(id));
        assertThrows(NoSuchElementException.class, () -> tratamentos.historico(id, 0, 20));
        assertThrows(NoSuchElementException.class, () -> tratamentos.revisar(id,
                new RevisaoTratamentoDTO(DecisaoRevisao.CONFIRMAR, null, "Tentativa cruzada", 0L)));
        assertThrows(NoSuchElementException.class, () -> tratamentos.atualizar(id,
                new AtualizacaoTratamentoDTO(null, null, StatusTratamento.ABERTA, 0L)));
        assertThrows(NoSuchElementException.class, () -> tratamentos.abrir(new AberturaTratamentoDTO(avaliacaoId)));
    }

    @Test void gestorDeveRevisarSomenteSeuDepartamentoMesmoManipulandoIds() {
        var proprio = abrir();
        var departamento = new Departamento(null, "Outro", null, null, empresa); em.persist(departamento);
        var outro = novoUsuario(empresa, departamento, "outro@example.test");
        var p = politicas.criar(dto("Outra política", true));
        var a = avaliarCom(registroPara(outro, ferramenta), p.getId(), Set.of());
        var externo = tratamentos.abrir(new AberturaTratamentoDTO(a.id()));
        var gestor = em.find(Usuario.class, usuario.getId());
        gestor.setPerfil(PerfilUsuario.GESTOR);
        negarId(externo.tratamento().id(), a.id());
        assertEquals(1, tratamentos.listar(null, 0, 20).totalItens());
        tratamentos.revisar(proprio.tratamento().id(), new RevisaoTratamentoDTO(
                DecisaoRevisao.CONFIRMAR, null, "No próprio escopo", proprio.tratamento().versaoRegistro()));
        assertEquals(2, tratamentos.historico(proprio.tratamento().id(), 0, 20).totalItens());
        gestor.setPerfil(PerfilUsuario.ADMIN);
        assertEquals(1, tratamentos.historico(externo.tratamento().id(), 0, 20).totalItens());
    }

    @Test void adminDeOutraEmpresaNaoPodeLerNemAlterarMesmoComIdValido() {
        var original = abrir();
        var outra = new Empresa(null, "Outra", "00000000000004", null, null); em.persist(outra);
        var d = new Departamento(null, "Outro", null, null, outra); em.persist(d);
        var admin = novoUsuario(outra, d, "externo@example.test");
        contexto.definir(new ContextoEmpresa(admin.getId(), outra.getId()));
        negarId(original.tratamento().id(), original.avaliacao().id());
        assertEquals(0, tratamentos.listar(null, 0, 20).totalItens());
        contexto.definir(new ContextoEmpresa(usuario.getId(), empresa.getId()));
        assertEquals(1, tratamentos.historico(original.tratamento().id(), 0, 20).totalItens());
    }

    private void negarPerfil(Long id) {
        assertEquals(403, assertThrows(ResponseStatusException.class, () -> tratamentos.historico(id, 0, 20)).getStatusCode().value());
        assertEquals(403, assertThrows(ResponseStatusException.class, () -> tratamentos.revisar(id,
                new RevisaoTratamentoDTO(DecisaoRevisao.CONFIRMAR, null, "Não autorizado", 0L))).getStatusCode().value());
    }

    @Test void deveBloquearFuncionarioInativoGestorSemDepartamentoEContextoForjado() {
        var t = abrir().tratamento();
        var autor = em.find(Usuario.class, usuario.getId());
        autor.setPerfil(PerfilUsuario.FUNCIONARIO);
        negarPerfil(t.id());
        autor.setPerfil(PerfilUsuario.ADMIN); autor.setAtivo(false);
        negarPerfil(t.id());
        autor.setAtivo(true); autor.setPerfil(PerfilUsuario.GESTOR);
        var departamento = autor.getDepartamento(); autor.setDepartamento(null);
        negarPerfil(t.id());
        autor.setDepartamento(departamento); autor.setPerfil(PerfilUsuario.ADMIN);
        contexto.definir(new ContextoEmpresa(usuario.getId(), Long.MAX_VALUE));
        negarPerfil(t.id());
        contexto.definir(new ContextoEmpresa(Long.MAX_VALUE, empresa.getId()));
        negarPerfil(t.id());
        contexto.definir(new ContextoEmpresa(usuario.getId(), empresa.getId()));
        assertEquals(1, tratamentos.historico(t.id(), 0, 20).totalItens());
    }

    @Test void devePaginarHistoricoEIgnorarAtualizacaoSemMudanca() {
        var t = abrir().tratamento();
        tratamentos.atualizar(t.id(), new AtualizacaoTratamentoDTO(null, null, StatusTratamento.ABERTA, t.versaoRegistro()));
        assertEquals(1, tratamentos.historico(t.id(), 0, 20).totalItens());
        var mudanca = tratamentos.atualizar(t.id(), new AtualizacaoTratamentoDTO(usuario.getId(),
                "Ação", StatusTratamento.EM_TRATAMENTO, t.versaoRegistro()));
        tratamentos.atualizar(t.id(), new AtualizacaoTratamentoDTO(usuario.getId(),
                "Ação", StatusTratamento.EM_TRATAMENTO, mudanca.tratamento().versaoRegistro()));
        assertEquals(2, tratamentos.historico(t.id(), 0, 20).totalItens());
        var pagina = tratamentos.historico(t.id(), 0, 1);
        assertEquals(2, pagina.totalPaginas());
        assertEquals(TipoEventoTratamento.ATUALIZACAO, pagina.itens().getFirst().tipo());
        assertEquals(TipoEventoTratamento.ABERTURA, tratamentos.historico(t.id(), 1, 1).itens().getFirst().tipo());
        assertEquals(400, assertThrows(ResponseStatusException.class, () -> tratamentos.historico(t.id(), -1, 20)).getStatusCode().value());
        assertEquals(400, assertThrows(ResponseStatusException.class, () -> tratamentos.historico(t.id(), 0, 101)).getStatusCode().value());
        assertThrows(NoSuchElementException.class, () -> tratamentos.historico(Long.MAX_VALUE, 0, 20));
    }

    @Test void deveIntegrarDecisaoVigenteAoIndicadorSemAlterarOChecklistOriginal() {
        var original = abrir();
        var hoje = java.time.LocalDate.now();
        var antes = indicadores.consultar(usuario, hoje, hoje,
                br.com.hestia.indicadores.service.ConsultaConfiancaIndicadoresService.Visao.PESSOAL, null);
        assertEquals(0, antes.indicadores().get(2).valor().signum());
        var revisado = tratamentos.revisar(original.tratamento().id(), new RevisaoTratamentoDTO(
                DecisaoRevisao.REVISAR, StatusConformidade.CONFORME, "Autorização comprovada", original.tratamento().versaoRegistro()));
        var depois = indicadores.consultar(usuario, hoje, hoje,
                br.com.hestia.indicadores.service.ConsultaConfiancaIndicadoresService.Visao.PESSOAL, null);
        assertEquals(0, java.math.BigDecimal.valueOf(100).compareTo(depois.indicadores().get(2).valor()));
        assertEquals("PARCIAL", depois.indicadores().get(2).confianca().classificacao());
        assertEquals(original.avaliacao(), tratamentos.buscar(original.tratamento().id()).avaliacao());
        tratamentos.revisar(original.tratamento().id(), new RevisaoTratamentoDTO(
                DecisaoRevisao.REVISAR, StatusConformidade.REQUER_REVISAO, "Evidências adicionais necessárias", revisado.tratamento().versaoRegistro()));
        var pendente = indicadores.consultar(usuario, hoje, hoje,
                br.com.hestia.indicadores.service.ConsultaConfiancaIndicadoresService.Visao.PESSOAL, null);
        assertNull(pendente.indicadores().get(2).valor());
        assertEquals(0, pendente.indicadores().get(2).confianca().completude().signum());
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
