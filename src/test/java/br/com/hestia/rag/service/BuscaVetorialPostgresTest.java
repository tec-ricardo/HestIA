package br.com.hestia.rag.service;

import br.com.hestia.autenticacao.contexto.ContextoEmpresa;
import br.com.hestia.autenticacao.contexto.EmpresaAtual;
import br.com.hestia.empresa.model.Empresa;
import br.com.hestia.reutilizavel.model.RespostaReutilizavel;
import br.com.hestia.reutilizavel.repository.RespostaReutilizavelRepository;
import br.com.hestia.reutilizavel.service.AcessoRespostaService;
import br.com.hestia.usuario.model.PerfilUsuario;
import br.com.hestia.usuario.model.Usuario;
import br.com.hestia.usuario.repository.UsuarioRepository;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@EnabledIfEnvironmentVariable(named = "HESTIA_PG_TEST_URL", matches = ".+")
class BuscaVetorialPostgresTest {

    private JdbcTemplate jdbc;
    private Flyway flyway;
    private BuscaVetorialService service;
    private EmbeddingService embeddings;
    private Usuario usuario;
    private RespostaReutilizavelRepository repository;
    private final Map<Long, RespostaReutilizavel> entities = new HashMap<>();
    private String schema;

    @BeforeEach
    void prepare() {
        var ds = new DriverManagerDataSource(
                System.getenv("HESTIA_PG_TEST_URL"),
                System.getenv("HESTIA_PG_TEST_USER"),
                System.getenv("HESTIA_PG_TEST_PASSWORD")
        );

        schema = "d2_" + UUID.randomUUID().toString().replace("-", "");

        ds.setUrl(
                ds.getUrl()
                        + (ds.getUrl().contains("?") ? "&" : "?")
                        + "currentSchema=" + schema + ",public"
        );

        jdbc = new JdbcTemplate(ds);
        jdbc.execute("CREATE SCHEMA " + schema);

        flyway = Flyway.configure()
                .dataSource(ds)
                .schemas(schema)
                .defaultSchema(schema)
                .locations("classpath:db/migration")
                .load();

        flyway.migrate();

        usuario = new Usuario();
        usuario.setId(1L);
        usuario.setAtivo(true);
        usuario.setPerfil(PerfilUsuario.FUNCIONARIO);
        usuario.setEmpresa(
                new Empresa(1L, "Teste", "12345678901234", null, null)
        );

        var contexto = new EmpresaAtual();
        contexto.definir(new ContextoEmpresa(1L, 1L));

        var usuarios = mock(UsuarioRepository.class);
        when(usuarios.findById(1L)).thenReturn(Optional.of(usuario));

        repository = mock(RespostaReutilizavelRepository.class);

        when(repository.findById(anyLong())).thenAnswer(
                i -> Optional.ofNullable(entities.get(i.getArgument(0)))
        );

        when(repository.findAllById(any())).thenAnswer(i -> {
            Iterable<Long> ids = i.getArgument(0);
            List<RespostaReutilizavel> found = new ArrayList<>();

            for (Long id : ids) {
                if (entities.containsKey(id)) {
                    found.add(entities.get(id));
                }
            }

            return found;
        });

        embeddings = mock(EmbeddingService.class);

        when(embeddings.gerarEmbedding(anyString()))
                .thenReturn(vetorDeTeste());

        service = new BuscaVetorialService(
                jdbc,
                embeddings,
                repository,
                contexto,
                usuarios,
                new AcessoRespostaService(repository)
        );
    }

    @AfterEach
    void cleanup() {
        if (jdbc != null && schema != null) {
            jdbc.execute("DROP SCHEMA " + schema + " CASCADE");
        }
    }

    private List<Float> vetorDeTeste() {
        var vector = new ArrayList<>(Collections.nCopies(1536, 0f));
        vector.set(0, 1f);
        return vector;
    }

    private long row(
            long company,
            PerfilUsuario profile,
            boolean sensitive,
            boolean reusable,
            double first,
            double second
    ) {
        var vector = new ArrayList<>(Collections.nCopies(1536, "0"));
        vector.set(0, Double.toString(first));
        vector.set(1, Double.toString(second));

        Long id = jdbc.queryForObject(
                """
                INSERT INTO respostas_reutilizaveis
                (empresa_id, prompt_original, resposta_gerada,
                 possui_dados_sensiveis, reutilizavel, embedding_vector)
                VALUES (?, 'Pergunta de teste', 'Conteúdo de teste',
                        ?, ?, CAST(? AS vector))
                RETURNING id
                """,
                Long.class,
                company,
                sensitive,
                reusable,
                "[" + String.join(",", vector) + "]"
        );

        if (profile != null) {
            jdbc.update(
                    "INSERT INTO resposta_perfis VALUES (?, ?)",
                    id,
                    profile.name()
            );
        }

        var entity = new RespostaReutilizavel();
        ReflectionTestUtils.setField(entity, "id", id);
        entity.setEmpresaId(company);
        entity.setPromptOriginal("Pergunta de teste");
        entity.setRespostaGerada("Conteúdo de teste");
        entity.setReutilizavel(reusable);
        entity.setPossuiDadosSensiveis(sensitive);
        entity.setPerfisPermitidos(
                profile == null ? Set.of() : Set.of(profile)
        );

        entities.put(id, entity);
        return id;
    }

    @Test
    void migratesAllVersionsAndCreatesVectorIndexAndStaging() {
        assertThat(flyway.info().applied()).hasSize(23);
        assertThat(flyway.validateWithResult().validationSuccessful).isTrue();
        assertThat(flyway.migrate().migrationsExecuted).isZero();

        assertThat(jdbc.queryForObject(
                "SELECT extversion FROM pg_extension WHERE extname='vector'",
                String.class
        )).isNotBlank();

        assertThat(jdbc.queryForObject(
                """
                SELECT count(*)
                FROM pg_indexes
                WHERE schemaname=?
                  AND indexname='idx_respostas_embedding_vector'
                """,
                Integer.class,
                schema
        )).isEqualTo(1);

        assertThat(jdbc.queryForObject(
                "SELECT to_regclass('base_sintetica.import_linhas')::text",
                String.class
        )).isEqualTo("base_sintetica.import_linhas");
    }

    @Test
    void statusEValidadeFiltramAntesDoLimiteComRelogioDaAplicacao(){
        long bloqueada=row(1,PerfilUsuario.FUNCIONARIO,false,true,1,0);
        long expirada=row(1,PerfilUsuario.FUNCIONARIO,false,true,1,0);
        long aprovada=row(1,PerfilUsuario.FUNCIONARIO,false,true,0.95,0.1);
        jdbc.update("UPDATE respostas_reutilizaveis SET status_qualidade='BLOQUEADA' WHERE id=?",bloqueada);
        jdbc.update("UPDATE respostas_reutilizaveis SET expira_em=? WHERE id=?",java.time.LocalDateTime.now().minusMinutes(1),expirada);
        var futura=java.time.LocalDateTime.now().plusMinutes(10);
        jdbc.update("UPDATE respostas_reutilizaveis SET expira_em=? WHERE id=?",futura,aprovada);
        entities.get(aprovada).setExpiraEm(futura);
        assertThat(service.buscarSemelhantes(1L,"Pergunta de teste",1)).extracting(RespostaReutilizavel::getId).containsExactly(aprovada);
    }

    @Test
    void filtersBeforeLimitEvenWhenForbiddenRowsAreCloser() {
        row(1, PerfilUsuario.GESTOR, false, true, 1, 0);
        row(2, PerfilUsuario.FUNCIONARIO, false, true, 1, 0);
        row(1, null, false, true, 1, 0);
        row(1, PerfilUsuario.FUNCIONARIO, true, true, 1, 0);
        row(1, PerfilUsuario.FUNCIONARIO, false, false, 1, 0);

        long allowed = row(
                1, PerfilUsuario.FUNCIONARIO, false, true, 1, 1
        );

        assertThat(service.buscarSemelhantes(1L, "pergunta", 1))
                .extracting(RespostaReutilizavel::getId)
                .containsExactly(allowed);
    }

    @Test
    void noPermissionsProducesEmptyResultsEvenForAdmin() {
        usuario.setPerfil(PerfilUsuario.ADMIN);
        row(1, PerfilUsuario.FUNCIONARIO, false, true, 1, 0);

        assertThat(service.buscarSemelhantes(1L, "pergunta", 10)).isEmpty();
    }

    @Test
    void rejectsInactiveUsersAndForeignCompaniesBeforeEmbedding() {
        assertThatThrownBy(
                () -> service.buscarSemelhantes(2L, "pergunta", 1)
        ).isInstanceOf(IllegalArgumentException.class);

        usuario.setAtivo(false);

        assertThatThrownBy(
                () -> service.buscarSemelhantes(1L, "pergunta", 1)
        ).isInstanceOf(ResponseStatusException.class);

        verifyNoInteractions(embeddings);
    }

    @Test
    void indexingRequiresProfileAndPreservesForbiddenVector() {
        long forbidden = row(
                1, PerfilUsuario.GESTOR, false, true, 0, 1
        );

        assertThatThrownBy(
                () -> service.indexarResposta(1L, forbidden)
        ).isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(embeddings);

        long allowed = row(
                1, PerfilUsuario.FUNCIONARIO, false, true, 0, 1
        );

        service.indexarResposta(1L, allowed);

        assertThat(jdbc.queryForObject(
                """
                SELECT vector_dims(embedding_vector)
                FROM respostas_reutilizaveis
                WHERE id=?
                """,
                Integer.class,
                allowed
        )).isEqualTo(1536);

        assertThat(jdbc.queryForObject(
                "SELECT embedding FROM respostas_reutilizaveis WHERE id=?",
                String.class,
                allowed
        )).isNotBlank();

        assertThat(service.buscarSemelhantes(1L, "pergunta", 1))
                .extracting(RespostaReutilizavel::getId)
                .containsExactly(allowed);
    }

    @Test
    void rejectsEmbeddingWhenContentChangesDuringGeneration() {
        long id = row(
                1, PerfilUsuario.FUNCIONARIO, false, true, 0, 1
        );

        String vetorAnterior = jdbc.queryForObject(
                "SELECT embedding_vector::text FROM respostas_reutilizaveis WHERE id=?",
                String.class,
                id
        );

        when(embeddings.gerarEmbedding("Pergunta de teste"))
                .thenAnswer(invocation -> {
                    jdbc.update(
                            """
                            UPDATE respostas_reutilizaveis
                            SET prompt_original = 'Pergunta alterada'
                            WHERE id = ?
                            """,
                            id
                    );
                    return vetorDeTeste();
                });

        assertThatThrownBy(() -> service.indexarResposta(1L, id))
                .isInstanceOfSatisfying(
                        ResponseStatusException.class,
                        erro -> assertThat(erro.getStatusCode().value())
                                .isEqualTo(409)
                );

        assertThat(jdbc.queryForObject(
                "SELECT embedding_vector::text FROM respostas_reutilizaveis WHERE id=?",
                String.class,
                id
        )).isEqualTo(vetorAnterior);
    }
}
