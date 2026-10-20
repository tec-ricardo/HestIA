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
class LimiteSimilaridadePostgresTest {

    private final Map<Long, RespostaReutilizavel> respostas = new HashMap<>();

    private JdbcTemplate jdbc;
    private BuscaVetorialService service;
    private EmbeddingService embeddings;
    private String schema;

    @BeforeEach
    void preparar() {
        String url = System.getenv("HESTIA_PG_TEST_URL");
        schema = "similaridade_" + UUID.randomUUID()
                .toString().replace("-", "");

        var ds = new DriverManagerDataSource(
                url + (url.contains("?") ? "&" : "?")
                        + "currentSchema=" + schema + ",public",
                System.getenv("HESTIA_PG_TEST_USER"),
                System.getenv("HESTIA_PG_TEST_PASSWORD")
        );

        jdbc = new JdbcTemplate(ds);
        jdbc.execute("CREATE SCHEMA " + schema);

        Flyway.configure()
                .dataSource(ds)
                .schemas(schema)
                .defaultSchema(schema)
                .locations("classpath:db/migration")
                .load()
                .migrate();

        var usuario = new Usuario();
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

        var repository = mock(RespostaReutilizavelRepository.class);

        when(repository.findAllById(any())).thenAnswer(invocation -> {
            Iterable<Long> ids = invocation.getArgument(0);
            List<RespostaReutilizavel> resultado = new ArrayList<>();

            for (Long id : ids) {
                if (respostas.containsKey(id)) {
                    resultado.add(respostas.get(id));
                }
            }

            return resultado;
        });

        embeddings = mock(EmbeddingService.class);

        var vetorConsulta = new ArrayList<>(
                Collections.nCopies(1536, 0f)
        );
        vetorConsulta.set(0, 1f);

        when(embeddings.gerarEmbedding(anyString()))
                .thenReturn(vetorConsulta);

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
    void limpar() {
        if (jdbc != null && schema != null) {
            jdbc.execute("DROP SCHEMA " + schema + " CASCADE");
        }
    }

    @Test
    void deveOrdenarResultadosERemoverOsAbaixoDoLimite() {
        long exata = inserir(1, 0);
        long intermediaria = inserir(1, 1);
        inserir(0, 1);

        assertThat(service.buscarSemelhantes(
                1L, "Pergunta", 20, 0.70
        ))
                .extracting(RespostaReutilizavel::getId)
                .containsExactly(exata, intermediaria);
    }

    @Test
    void limiteMaisAltoDeveExcluirResultadoIntermediario() {
        long exata = inserir(1, 0);
        inserir(1, 1);

        assertThat(service.buscarSemelhantes(
                1L, "Pergunta", 20, 0.90
        ))
                .extracting(RespostaReutilizavel::getId)
                .containsExactly(exata);
    }

    @Test
    void deveRetornarVazioQuandoNenhumResultadoAtendeAoLimite() {
        inserir(0, 1);

        assertThat(service.buscarSemelhantes(
                1L, "Pergunta", 5, 0.70
        )).isEmpty();
    }

    @Test
    void deveRespeitarQuantidadeMaximaDeResultados() {
        long exata = inserir(1, 0);
        inserir(1, 1);

        assertThat(service.buscarSemelhantes(
                1L, "Pergunta", 1, 0.70
        ))
                .extracting(RespostaReutilizavel::getId)
                .containsExactly(exata);
    }

    @Test
    void deveRejeitarParametrosInvalidosAntesDeGerarEmbedding() {
        for (double limite : new double[]{
                -0.1, 1.1, Double.NaN, Double.POSITIVE_INFINITY
        }) {
            assertThatThrownBy(() -> service.buscarSemelhantes(
                    1L, "Pergunta", 5, limite
            )).isInstanceOf(ResponseStatusException.class);
        }

        assertThatThrownBy(() -> service.buscarSemelhantes(
                1L, " ", 5, 0.70
        )).isInstanceOf(ResponseStatusException.class);

        assertThatThrownBy(() -> service.buscarSemelhantes(
                1L, "Pergunta", 0, 0.70
        )).isInstanceOf(ResponseStatusException.class);

        assertThatThrownBy(() -> service.buscarSemelhantes(
                1L, "Pergunta", 21, 0.70
        )).isInstanceOf(ResponseStatusException.class);

        verifyNoInteractions(embeddings);
    }

    private long inserir(double primeiro, double segundo) {
        var valores = new ArrayList<>(
                Collections.nCopies(1536, "0")
        );
        valores.set(0, Double.toString(primeiro));
        valores.set(1, Double.toString(segundo));

        String vetor = "[" + String.join(",", valores) + "]";

        Long id = jdbc.queryForObject(
                """
                INSERT INTO respostas_reutilizaveis (
                    empresa_id, prompt_original, resposta_gerada,
                    embedding, embedding_vector
                )
                VALUES (1, 'Pergunta', 'Resposta', ?, CAST(? AS vector))
                RETURNING id
                """,
                Long.class,
                vetor,
                vetor
        );

        jdbc.update(
                """
                INSERT INTO resposta_perfis (resposta_id, perfil)
                VALUES (?, 'FUNCIONARIO')
                """,
                id
        );

        var resposta = new RespostaReutilizavel();
        ReflectionTestUtils.setField(resposta, "id", id);
        resposta.setEmpresaId(1L);
        resposta.setPromptOriginal("Pergunta");
        resposta.setRespostaGerada("Resposta");
        resposta.setPerfisPermitidos(Set.of(PerfilUsuario.FUNCIONARIO));

        respostas.put(id, resposta);
        return id;
    }
}