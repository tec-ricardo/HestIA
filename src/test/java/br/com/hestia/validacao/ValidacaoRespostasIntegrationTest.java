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
import org.springframework.http.MediaType;

@SpringBootTest
@Transactional
class ValidacaoRespostasIntegrationTest {
    private static final String PG_URL = System.getenv("HESTIA_PG_TEST_URL");
    private static final String SCHEMA = "qualidade_" + UUID.randomUUID().toString().replace("-", "");
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
        mvc = org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup(web).build();
        if (PG_URL == null || PG_URL.isBlank())
            jdbc.execute("ALTER TABLE respostas_reutilizaveis ADD COLUMN IF NOT EXISTS embedding_vector VARCHAR");
        var vetor = new ArrayList<Float>(Collections.nCopies(1536, 0f)); vetor.set(0, 1f);
        org.mockito.Mockito.when(embeddings.gerarEmbedding(org.mockito.ArgumentMatchers.anyString())).thenReturn(vetor);
    }

    @AfterEach
    void limparContexto() { RequestContextHolder.resetRequestAttributes(); }


    @Autowired br.com.hestia.reutilizavel.service.AcessoRespostaService acesso;
    @Autowired br.com.hestia.reutilizavel.service.RespostaReutilizavelService respostas;
    @Autowired br.com.hestia.rag.service.ManutencaoConteudoService manutencao;
    @Autowired org.springframework.jdbc.core.JdbcTemplate jdbc;
    @Autowired br.com.hestia.autenticacao.service.AutenticacaoService autenticacao;
    @Autowired org.springframework.security.crypto.password.PasswordEncoder encoder;
    @Autowired org.springframework.web.context.WebApplicationContext web;
    @org.springframework.test.context.bean.override.mockito.MockitoBean
    br.com.hestia.rag.service.EmbeddingService embeddings;
    // Auditoria usa REQUIRES_NEW e não enxerga as contas ainda não commitadas das fixtures.
    // A retirada documentada é validada nos próprios campos persistidos, na transação de teste.
    @org.springframework.test.context.bean.override.mockito.MockitoBean
    br.com.hestia.auditoria.service.AuditoriaService auditoria;
    private org.springframework.test.web.servlet.MockMvc mvc;

    private String token(Usuario u) {
        var atual = em.find(Usuario.class, u.getId());
        atual.setSenha(encoder.encode("credencial-exclusiva-do-teste"));
        em.flush();
        return autenticacao.autenticar(new br.com.hestia.autenticacao.dto.LoginDTO(
                atual.getEmail(), "credencial-exclusiva-do-teste")).token();
    }

    private br.com.hestia.reutilizavel.model.RespostaReutilizavel resposta(Set<PerfilUsuario> perfis) {
        contexto.definir(new ContextoEmpresa(usuario.getId(), empresa.getId()));
        var dto = new br.com.hestia.reutilizavel.dto.RespostaReutilizavelDTO();
        dto.setEmpresaId(empresa.getId()); dto.setPromptOriginal("Pergunta de qualidade");
        dto.setRespostaGerada("Conteúdo inicial de teste"); dto.setTokensEntrada(100); dto.setTokensSaida(20);
        dto.setCustoEstimado(new java.math.BigDecimal("0.05"));
        var r = manutencao.cadastrar(dto, em.find(Usuario.class, usuario.getId()));
        acesso.autorizarPerfis(usuario, r.getId(), perfis);
        em.flush();
        return r;
    }

    @Test void deveRevalidarPermissaoRevogadaDepoisDaSelecaoAntesDeRegistrarUso() {
        var r = resposta(Set.of(PerfilUsuario.ADMIN, PerfilUsuario.FUNCIONARIO));
        var leitor = novoUsuario(empresa, usuario.getDepartamento(), "leitor@example.test");
        leitor.setPerfil(PerfilUsuario.FUNCIONARIO); em.flush();
        assertNotNull(acesso.buscarPorId(leitor, r.getId()));
        acesso.autorizarPerfis(usuario, r.getId(), Set.of(PerfilUsuario.ADMIN));
        contexto.definir(new ContextoEmpresa(leitor.getId(), empresa.getId()));
        assertThrows(ResponseStatusException.class, () -> respostas.registrarReutilizacao(r.getId(), leitor));
        assertEquals(0, em.find(br.com.hestia.reutilizavel.model.RespostaReutilizavel.class, r.getId()).getQuantidadeReutilizacoes());
    }

    @Test void deveRevisarRetirarComJustificativaEPreservarConteudoSemNovosUsos() throws Exception {
        var r = resposta(Set.of(PerfilUsuario.ADMIN, PerfilUsuario.FUNCIONARIO));
        var leitor = novoUsuario(empresa, usuario.getDepartamento(), "leitor@example.test");
        leitor.setPerfil(PerfilUsuario.FUNCIONARIO);
        String adminToken = token(usuario), leitorToken = token(leitor);
        mvc.perform(put("/api/respostas-reutilizaveis/" + r.getId()).header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"promptOriginal\":\"Pergunta revisada\",\"respostaGerada\":\"Conteúdo revisado\",\"origem\":\"Política de teste\",\"tipoConteudo\":\"RESPOSTA\"}"))
                .andExpect(status().isOk());
        mvc.perform(get("/api/respostas-reutilizaveis/" + r.getId() + "/revisao").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.versoes.length()").value(2))
                .andExpect(jsonPath("$.versoes[1].respostaGerada").value("Conteúdo inicial de teste"));
        mvc.perform(patch("/api/respostas-reutilizaveis/" + r.getId() + "/reutilizar")
                .header("Authorization", "Bearer " + leitorToken)).andExpect(status().isOk())
                .andExpect(jsonPath("$.quantidadeReutilizacoes").value(1));
        Long movimentos = jdbc.queryForObject("SELECT COUNT(*) FROM movimentos_experiencia", Long.class);
        String body = "{\"justificativa\":\"  Conteúdo desatualizado  \"}";
        mvc.perform(delete("/api/respostas-reutilizaveis/" + r.getId()).header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isNoContent());
        mvc.perform(delete("/api/respostas-reutilizaveis/" + r.getId()).header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON).content("{\"justificativa\":\"Tentativa repetida\"}"))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/respostas-reutilizaveis/" + r.getId() + "/revisao").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.retiradaPorId").value(usuario.getId()))
                .andExpect(jsonPath("$.justificativaRetirada").value("Conteúdo desatualizado"))
                .andExpect(jsonPath("$.retiradaDocumentada").value(true)).andExpect(jsonPath("$.retiradaEm").isNotEmpty())
                .andExpect(jsonPath("$.reutilizavel").value(false)).andExpect(jsonPath("$.versoes.length()").value(2));
        mvc.perform(get("/api/respostas-reutilizaveis/" + r.getId()).header("Authorization", "Bearer " + leitorToken))
                .andExpect(status().isNotFound());
        mvc.perform(patch("/api/respostas-reutilizaveis/" + r.getId() + "/reutilizar")
                .header("Authorization", "Bearer " + leitorToken)).andExpect(status().isNotFound());
        for (String caminho : List.of("/api/respostas-reutilizaveis",
                "/api/respostas-reutilizaveis/empresa/" + empresa.getId() + "/reutilizaveis",
                "/api/respostas-reutilizaveis/empresa/" + empresa.getId() + "/buscar?prompt=Pergunta")) {
            mvc.perform(get(caminho).header("Authorization", "Bearer " + leitorToken))
                    .andExpect(status().isOk()).andExpect(content().json("[]"));
        }
        assertEquals(movimentos, jdbc.queryForObject("SELECT COUNT(*) FROM movimentos_experiencia", Long.class));
        assertEquals(1, jdbc.queryForObject("SELECT quantidade_reutilizacoes FROM respostas_reutilizaveis WHERE id=?", Integer.class, r.getId()));
        assertEquals(120L, jdbc.queryForObject("SELECT tokens_economizados FROM respostas_reutilizaveis WHERE id=?", Long.class, r.getId()));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM respostas_reutilizaveis WHERE id=? AND embedding IS NULL AND embedding_vector IS NULL", Integer.class, r.getId()));
    }

    @Test void deveNegarLeituraAlteracaoRetiradaEPerfisDeOutraEmpresa() throws Exception {
        var r = resposta(Set.of(PerfilUsuario.ADMIN, PerfilUsuario.FUNCIONARIO));
        var outra = new Empresa(null, "Outra", "00000000000004", null, null); em.persist(outra);
        var departamento = new Departamento(null, "Outro", null, null, outra); em.persist(departamento);
        var outroAdmin = novoUsuario(outra, departamento, "externo@example.test");
        String token = token(outroAdmin);
        for (String sufixo : List.of("", "/revisao")) {
            mvc.perform(get("/api/respostas-reutilizaveis/" + r.getId() + sufixo).header("Authorization", "Bearer " + token))
                    .andExpect(status().isNotFound());
        }
        mvc.perform(patch("/api/respostas-reutilizaveis/" + r.getId() + "/reutilizar").header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/api/respostas-reutilizaveis/" + r.getId()).header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content("{\"justificativa\":\"Tentativa cruzada\"}"))
                .andExpect(status().isNotFound());
        mvc.perform(put("/api/respostas-reutilizaveis/" + r.getId() + "/perfis").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content("[\"ADMIN\"]")).andExpect(status().isNotFound());
        mvc.perform(get("/api/respostas-reutilizaveis/empresa/" + empresa.getId() + "/reutilizaveis")
                .header("Authorization", "Bearer " + token)).andExpect(status().isForbidden());
        assertTrue(em.find(br.com.hestia.reutilizavel.model.RespostaReutilizavel.class, r.getId()).getReutilizavel());
    }

    @Test void deveNegarPerfilSemPermissaoEImpedirEscaladaDePrivilegios() throws Exception {
        var r = resposta(Set.of(PerfilUsuario.ADMIN));
        var leitor = novoUsuario(empresa, usuario.getDepartamento(), "funcionario@example.test");
        leitor.setPerfil(PerfilUsuario.FUNCIONARIO);
        String token = token(leitor);
        mvc.perform(get("/api/respostas-reutilizaveis/" + r.getId()).header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
        mvc.perform(patch("/api/respostas-reutilizaveis/" + r.getId() + "/reutilizar").header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/respostas-reutilizaveis/" + r.getId() + "/revisao").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
        mvc.perform(put("/api/respostas-reutilizaveis/" + r.getId() + "/perfis").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content("[\"FUNCIONARIO\"]")).andExpect(status().isForbidden());
        mvc.perform(delete("/api/respostas-reutilizaveis/" + r.getId()).header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content("{\"justificativa\":\"Tentativa sem permissão\"}"))
                .andExpect(status().isForbidden());
        assertEquals(Set.of(PerfilUsuario.ADMIN), em.find(br.com.hestia.reutilizavel.model.RespostaReutilizavel.class, r.getId()).getPerfisPermitidos());
    }

    @Test void deveBloquearSensivelRetiradoOuIdInexistenteSemContabilizarRecompensa() throws Exception {
        var sensivel = resposta(Set.of(PerfilUsuario.ADMIN));
        sensivel.setPossuiDadosSensiveis(true);
        var bloqueada = resposta(Set.of(PerfilUsuario.ADMIN)); bloqueada.setReutilizavel(false); em.flush();
        String token = token(usuario);
        for (Long id : List.of(sensivel.getId(), bloqueada.getId(), Long.MAX_VALUE, -1L)) {
            mvc.perform(get("/api/respostas-reutilizaveis/" + id).header("Authorization", "Bearer " + token))
                    .andExpect(status().isNotFound());
            mvc.perform(patch("/api/respostas-reutilizaveis/" + id + "/reutilizar").header("Authorization", "Bearer " + token))
                    .andExpect(status().isNotFound());
        }
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM movimentos_experiencia", Integer.class));
        assertEquals(0L, jdbc.queryForObject("SELECT COALESCE(SUM(quantidade_reutilizacoes),0) FROM respostas_reutilizaveis", Long.class));
    }

    @Test void deveExigirSessaoEJustificativaSemInventarAutoriaLegada() throws Exception {
        var r = resposta(Set.of(PerfilUsuario.ADMIN));
        mvc.perform(get("/api/respostas-reutilizaveis/" + r.getId())).andExpect(status().isUnauthorized());
        mvc.perform(patch("/api/respostas-reutilizaveis/" + r.getId() + "/reutilizar")
                .header("Authorization", "Bearer inexistente")).andExpect(status().isUnauthorized());
        String token = token(usuario);
        for (String body : List.of("{}", "{\"justificativa\":\" \"}",
                "{\"justificativa\":\"" + "x".repeat(1001) + "\"}")) {
            mvc.perform(delete("/api/respostas-reutilizaveis/" + r.getId()).header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isBadRequest());
        }
        mvc.perform(delete("/api/respostas-reutilizaveis/" + r.getId()).header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
        assertTrue(em.find(br.com.hestia.reutilizavel.model.RespostaReutilizavel.class, r.getId()).getReutilizavel());
        var legado = resposta(Set.of(PerfilUsuario.ADMIN)); legado.setReutilizavel(false); em.flush();
        mvc.perform(get("/api/respostas-reutilizaveis/" + legado.getId() + "/revisao").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.retiradaDocumentada").value(false))
                .andExpect(jsonPath("$.retiradaPorId").isEmpty()).andExpect(jsonPath("$.retiradaEm").isEmpty());
        var atual = em.find(Usuario.class, usuario.getId()); atual.setAtivo(false); em.flush();
        mvc.perform(get("/api/respostas-reutilizaveis").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test void deveIgnorarAutorForjadoNoCadastroERejeitarEmbeddingDoCliente() throws Exception {
        String token = token(usuario);
        mvc.perform(post("/api/respostas-reutilizaveis").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content("{\"empresaId\":" + empresa.getId()
                + ",\"usuarioId\":999,\"promptOriginal\":\"Teste\",\"respostaGerada\":\"Teste\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.usuarioId").value(usuario.getId()));
        mvc.perform(post("/api/respostas-reutilizaveis").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content("{\"empresaId\":" + empresa.getId()
                + ",\"promptOriginal\":\"Teste\",\"respostaGerada\":\"Teste\",\"embedding\":\"[1,0]\"}"))
                .andExpect(status().isBadRequest());
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM respostas_reutilizaveis", Integer.class));
    }

    @Test void deveRetirarDasSugestoesVetoriaisERevogarPerfilComPgvectorReal() throws Exception {
        org.junit.jupiter.api.Assumptions.assumeTrue(PG_URL != null && !PG_URL.isBlank());
        var r = resposta(Set.of(PerfilUsuario.ADMIN, PerfilUsuario.FUNCIONARIO));
        var leitor = novoUsuario(empresa, usuario.getDepartamento(), "busca@example.test");
        leitor.setPerfil(PerfilUsuario.FUNCIONARIO);
        String adminToken = token(usuario), leitorToken = token(leitor);
        mvc.perform(post("/api/respostas-reutilizaveis/" + r.getId() + "/indexar").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());
        String pergunta = "{\"pergunta\":\"Pergunta de qualidade\",\"limite\":5,\"similaridadeMinima\":0.7,\"empresaId\":999}";
        mvc.perform(post("/api/respostas-reutilizaveis/buscar-semelhantes").header("Authorization", "Bearer " + leitorToken)
                .contentType(MediaType.APPLICATION_JSON).content(pergunta))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(r.getId()));
        mvc.perform(put("/api/respostas-reutilizaveis/" + r.getId() + "/perfis").header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON).content("[\"ADMIN\"]")).andExpect(status().isNoContent());
        mvc.perform(post("/api/respostas-reutilizaveis/buscar-semelhantes").header("Authorization", "Bearer " + leitorToken)
                .contentType(MediaType.APPLICATION_JSON).content(pergunta)).andExpect(status().isOk()).andExpect(content().json("[]"));
        mvc.perform(delete("/api/respostas-reutilizaveis/" + r.getId()).header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON).content("{\"justificativa\":\"Revisão demonstrou inadequação\"}"))
                .andExpect(status().isNoContent());
        mvc.perform(post("/api/respostas-reutilizaveis/buscar-semelhantes").header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON).content(pergunta)).andExpect(status().isOk()).andExpect(content().json("[]"));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM respostas_reutilizaveis WHERE id=? AND embedding IS NULL AND embedding_vector IS NULL",
                Integer.class, r.getId()));
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
