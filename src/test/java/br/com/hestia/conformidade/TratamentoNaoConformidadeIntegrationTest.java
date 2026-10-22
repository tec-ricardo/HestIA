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
class TratamentoNaoConformidadeIntegrationTest {
    private static final String PG_URL = System.getenv("HESTIA_PG_TEST_URL");
    private static final String SCHEMA = "tratamento_" + UUID.randomUUID().toString().replace("-", "");
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

    @Test
    void deveAbrirComAutorEConsultarOcorrenciaEFundamentosOriginais() {
        var p = politicas.criar(dto("Texto original", true));
        var a = avaliarCom(novoRegistro(), p.getId(), Set.of());
        var t = tratamentos.abrir(new AberturaTratamentoDTO(a.id()));
        assertNotNull(t.tratamento().id());
        assertEquals(StatusTratamento.ABERTA, t.tratamento().status());
        assertEquals(usuario.getId(), t.tratamento().criadoPorId());
        assertNotNull(t.tratamento().criadoEm());
        assertNull(t.tratamento().responsavelId());
        assertNull(t.tratamento().acaoCorretiva());
        assertEquals(a.registroUsoId(), t.ocorrencia().id());
        assertEquals(a.id(), t.avaliacao().id());
        assertEquals(a.motivo(), t.avaliacao().motivo());
        assertEquals(a.politicaVersaoId(), t.politicaAplicada().id());

        politicas.atualizar(p.getId(), dto("Texto novo", false));
        politicas.excluir(p.getId());
        em.flush(); em.clear();
        var leitura = tratamentos.buscar(t.tratamento().id());
        assertEquals("Texto original", leitura.politicaAplicada().conteudo());
        assertEquals(1, leitura.politicaAplicada().revisao());
        assertEquals(StatusConformidade.NAO_CONFORME, leitura.avaliacao().status());
        assertEquals(a.motivo(), leitura.avaliacao().motivo());
        var pagina = tratamentos.listar(StatusTratamento.ABERTA, 0, 20);
        assertEquals(1, pagina.totalItens());
        assertEquals(t.tratamento().id(), pagina.itens().getFirst().id());
        assertEquals(0, tratamentos.listar(StatusTratamento.RESOLVIDA, 0, 20).totalItens());
    }

    @Test
    void deveRecusarConformeEPendenteDeRevisaoSemAlterarAvaliacao() {
        var p = politicas.criar(dto("Politica", true));
        var conforme = avaliarCom(novoRegistro(), p.getId(), EnumSet.allOf(CriterioConformidade.class));
        var pendente = avaliarCom(novoRegistro(), p.getId(), EnumSet.of(CriterioConformidade.LGPD));
        assertEquals(StatusConformidade.CONFORME, conforme.status());
        assertEquals(StatusConformidade.REQUER_REVISAO, pendente.status());
        assertThrows(IllegalArgumentException.class,
                () -> tratamentos.abrir(new AberturaTratamentoDTO(conforme.id())));
        assertThrows(IllegalArgumentException.class,
                () -> tratamentos.abrir(new AberturaTratamentoDTO(pendente.id())));
        assertEquals(0, tratamentos.listar(null, 0, 20).totalItens());
        assertEquals(StatusConformidade.CONFORME, avaliacoes.buscar(conforme.id()).status());
    }

    @Test
    void deveImpedirDoisTratamentosParaMesmaAvaliacao() {
        var a = avaliarCom(novoRegistro(), politicas.criar(dto("Politica", true)).getId(), Set.of());
        tratamentos.abrir(new AberturaTratamentoDTO(a.id()));
        assertThrows(IllegalArgumentException.class,
                () -> tratamentos.abrir(new AberturaTratamentoDTO(a.id())));
        assertEquals(1, tratamentos.listar(null, 0, 20).totalItens());
    }

    @Test
    void deveAceitarOcorrenciaLegadaSemInventarPoliticaAplicada() {
        var p = politicas.criar(dto("Politica", true));
        var a = new AvaliacaoConformidade(novoRegistro(), p, StatusConformidade.NAO_CONFORME,
                Set.of(), "Ocorrencia legada");
        em.persist(a); em.flush();
        var t = tratamentos.abrir(new AberturaTratamentoDTO(a.getId()));
        em.flush(); em.clear();
        var leitura = tratamentos.buscar(t.tratamento().id());
        assertNull(leitura.politicaAplicada());
        assertFalse(leitura.avaliacao().fundamentosPreservados());
        assertEquals("Ocorrencia legada", leitura.avaliacao().justificativa());
        assertTrue(leitura.avaliacao().limitacoes().contains("legada"));
    }

    @Test
    void gestorDeveVerSomenteSeuDepartamentoEAdminTodaEmpresa() {
        var p = politicas.criar(dto("Politica", true));
        var a1 = avaliarCom(novoRegistro(), p.getId(), Set.of());
        var t1 = tratamentos.abrir(new AberturaTratamentoDTO(a1.id()));
        var outroDepartamento = new Departamento(null, "Outro", null, null, empresa);
        em.persist(outroDepartamento);
        var outro = novoUsuario(empresa, outroDepartamento, "outro@example.test");
        var a2 = avaliarCom(registroPara(outro, ferramenta), p.getId(), Set.of());
        var t2 = tratamentos.abrir(new AberturaTratamentoDTO(a2.id()));
        assertEquals(2, tratamentos.listar(null, 0, 20).totalItens());

        usuario.setPerfil(PerfilUsuario.GESTOR);
        assertEquals(1, tratamentos.listar(null, 0, 20).totalItens());
        assertEquals(t1.tratamento().id(), tratamentos.buscar(t1.tratamento().id()).tratamento().id());
        assertThrows(NoSuchElementException.class, () -> tratamentos.buscar(t2.tratamento().id()));
        assertThrows(NoSuchElementException.class,
                () -> tratamentos.abrir(new AberturaTratamentoDTO(a2.id())));
    }

    @Test
    void deveBloquearOutraEmpresaMesmoAlterandoIds() {
        var p = politicas.criar(dto("Politica", true));
        var a = avaliarCom(novoRegistro(), p.getId(), Set.of());
        var t = tratamentos.abrir(new AberturaTratamentoDTO(a.id()));
        var outraEmpresa = new Empresa(null, "Outra", "00000000000004", null, null);
        em.persist(outraEmpresa);
        var d = new Departamento(null, "Outro", null, null, outraEmpresa);
        em.persist(d);
        var admin = novoUsuario(outraEmpresa, d, "outra@example.test");
        contexto.definir(new ContextoEmpresa(admin.getId(), outraEmpresa.getId()));
        assertEquals(0, tratamentos.listar(null, 0, 20).totalItens());
        assertThrows(NoSuchElementException.class, () -> tratamentos.buscar(t.tratamento().id()));
        assertThrows(NoSuchElementException.class,
                () -> tratamentos.abrir(new AberturaTratamentoDTO(a.id())));
    }

    @Test
    void deveRecusarFuncionarioInativoEGestorSemDepartamento() {
        usuario.setPerfil(PerfilUsuario.FUNCIONARIO);
        assertEquals(403, assertThrows(ResponseStatusException.class,
                () -> tratamentos.listar(null, 0, 20)).getStatusCode().value());
        usuario.setPerfil(PerfilUsuario.ADMIN);
        usuario.setAtivo(false);
        assertEquals(403, assertThrows(ResponseStatusException.class,
                () -> tratamentos.listar(null, 0, 20)).getStatusCode().value());
        usuario.setAtivo(true);
        usuario.setPerfil(PerfilUsuario.GESTOR);
        usuario.setDepartamento(null);
        assertEquals(403, assertThrows(ResponseStatusException.class,
                () -> tratamentos.listar(null, 0, 20)).getStatusCode().value());
    }

    @Test
    void deveRecusarPaginacaoInvalidaEIdentificadoresInexistentes() {
        assertEquals(400, assertThrows(ResponseStatusException.class,
                () -> tratamentos.listar(null, -1, 20)).getStatusCode().value());
        assertEquals(400, assertThrows(ResponseStatusException.class,
                () -> tratamentos.listar(null, 0, 101)).getStatusCode().value());
        assertThrows(NoSuchElementException.class, () -> tratamentos.buscar(Long.MAX_VALUE));
        assertThrows(NoSuchElementException.class,
                () -> tratamentos.abrir(new AberturaTratamentoDTO(Long.MAX_VALUE)));
    }

    @Test
    void deveAtualizarPersistirResolverEReabrirSemReescreverOrigem() {
        var a = avaliarCom(novoRegistro(), politicas.criar(dto("Original", true)).getId(), Set.of());
        var aberto = tratamentos.abrir(new AberturaTratamentoDTO(a.id()));
        em.flush(); em.clear();
        var inicial = tratamentos.buscar(aberto.tratamento().id());
        var id = inicial.tratamento().id();
        var atualizado = tratamentos.atualizar(id, new AtualizacaoTratamentoDTO(usuario.getId(),
                "  Orientar uso da ferramenta  ", StatusTratamento.EM_TRATAMENTO, inicial.tratamento().versaoRegistro()));
        assertEquals("Orientar uso da ferramenta", atualizado.tratamento().acaoCorretiva());
        assertTrue(atualizado.tratamento().versaoRegistro() > inicial.tratamento().versaoRegistro());
        assertThrows(IllegalArgumentException.class, () -> tratamentos.atualizar(id,
                new AtualizacaoTratamentoDTO(null, null, StatusTratamento.ABERTA, inicial.tratamento().versaoRegistro())));
        var resolvido = tratamentos.atualizar(id, new AtualizacaoTratamentoDTO(usuario.getId(),
                "Orientação concluída", StatusTratamento.RESOLVIDA, atualizado.tratamento().versaoRegistro()));
        em.clear();
        var leitura = tratamentos.buscar(id);
        assertEquals(StatusTratamento.RESOLVIDA, leitura.tratamento().status());
        assertEquals(inicial.tratamento().criadoEm(), leitura.tratamento().criadoEm());
        assertEquals(inicial.tratamento().criadoPorId(), leitura.tratamento().criadoPorId());
        assertEquals(inicial.ocorrencia(), leitura.ocorrencia());
        assertEquals(inicial.avaliacao(), leitura.avaliacao());
        assertEquals(inicial.politicaAplicada(), leitura.politicaAplicada());
        assertEquals(1, tratamentos.listar(StatusTratamento.RESOLVIDA, 0, 20).totalItens());
        var reaberto = tratamentos.atualizar(id, new AtualizacaoTratamentoDTO(null, "  ",
                StatusTratamento.ABERTA, resolvido.tratamento().versaoRegistro()));
        assertNull(reaberto.tratamento().acaoCorretiva());
        assertNull(reaberto.tratamento().responsavelId());
    }

    @Test
    void deveExigirPlanoEValidarResponsavelEEscopoNaEdicao() {
        var a = avaliarCom(novoRegistro(), politicas.criar(dto("Original", true)).getId(), Set.of());
        var t = tratamentos.abrir(new AberturaTratamentoDTO(a.id())).tratamento();
        for (var status : List.of(StatusTratamento.EM_TRATAMENTO, StatusTratamento.RESOLVIDA)) {
            assertThrows(IllegalArgumentException.class, () -> tratamentos.atualizar(t.id(),
                    new AtualizacaoTratamentoDTO(null, "Ação", status, t.versaoRegistro())));
            assertThrows(IllegalArgumentException.class, () -> tratamentos.atualizar(t.id(),
                    new AtualizacaoTratamentoDTO(usuario.getId(), " ", status, t.versaoRegistro())));
        }
        var d = new Departamento(null, "Outro", null, null, empresa); em.persist(d);
        var outro = novoUsuario(empresa, d, "responsavel@example.test");
        outro.setAtivo(false);
        assertThrows(IllegalArgumentException.class, () -> tratamentos.atualizar(t.id(),
                new AtualizacaoTratamentoDTO(outro.getId(), "Ação", StatusTratamento.ABERTA, t.versaoRegistro())));
        outro.setAtivo(true);
        usuario.setPerfil(PerfilUsuario.GESTOR);
        assertThrows(NoSuchElementException.class, () -> tratamentos.atualizar(t.id(),
                new AtualizacaoTratamentoDTO(outro.getId(), "Ação", StatusTratamento.ABERTA, t.versaoRegistro())));
        contexto.definir(new ContextoEmpresa(outro.getId(), empresa.getId()));
        outro.setPerfil(PerfilUsuario.GESTOR);
        assertThrows(NoSuchElementException.class, () -> tratamentos.atualizar(t.id(),
                new AtualizacaoTratamentoDTO(null, null, StatusTratamento.ABERTA, t.versaoRegistro())));
        contexto.definir(new ContextoEmpresa(usuario.getId(), empresa.getId()));
        usuario.setPerfil(PerfilUsuario.FUNCIONARIO);
        assertEquals(403, assertThrows(ResponseStatusException.class, () -> tratamentos.atualizar(t.id(),
                new AtualizacaoTratamentoDTO(null, null, StatusTratamento.ABERTA, t.versaoRegistro())))
                .getStatusCode().value());
        assertEquals(StatusTratamento.ABERTA, em.find(TratamentoNaoConformidade.class, t.id()).getStatus());
    }

    @Test
    void deveBloquearEdicaoEAtribuicaoEntreEmpresas() {
        var a = avaliarCom(novoRegistro(), politicas.criar(dto("Original", true)).getId(), Set.of());
        var t = tratamentos.abrir(new AberturaTratamentoDTO(a.id())).tratamento();
        var outraEmpresa = new Empresa(null, "Outra", "00000000000004", null, null);
        em.persist(outraEmpresa);
        var d = new Departamento(null, "Outro", null, null, outraEmpresa); em.persist(d);
        var outro = novoUsuario(outraEmpresa, d, "externo@example.test");
        assertThrows(NoSuchElementException.class, () -> tratamentos.atualizar(t.id(),
                new AtualizacaoTratamentoDTO(outro.getId(), "Ação", StatusTratamento.ABERTA, t.versaoRegistro())));
        contexto.definir(new ContextoEmpresa(outro.getId(), outraEmpresa.getId()));
        assertThrows(NoSuchElementException.class, () -> tratamentos.atualizar(t.id(),
                new AtualizacaoTratamentoDTO(outro.getId(), "Ação", StatusTratamento.ABERTA, t.versaoRegistro())));
        assertEquals(StatusTratamento.ABERTA, em.find(TratamentoNaoConformidade.class, t.id()).getStatus());
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
