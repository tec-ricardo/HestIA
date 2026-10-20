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

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@EnabledIfEnvironmentVariable(named = "HESTIA_PG_TEST_URL", matches = ".+")
class DesempenhoBuscaPostgresTest {

    private static final int TOTAL_REGISTROS = 1000;
    private static final int AMOSTRAS = 20;

    private JdbcTemplate jdbc;
    private BuscaVetorialService busca;
    private String schema;

    @BeforeEach
    void preparar() {
        String url = System.getenv("HESTIA_PG_TEST_URL");

        schema = "desempenho_" + UUID.randomUUID()
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

        var vetor = new ArrayList<>(Collections.nCopies(1536, 0f));
        vetor.set(0, 1f);
        String vetorTexto = vetor.toString();

        jdbc.update(
                """
                INSERT INTO respostas_reutilizaveis (
                    empresa_id, prompt_original, resposta_gerada,
                    reutilizavel, possui_dados_sensiveis,
                    embedding, embedding_vector
                )
                SELECT
                    CASE WHEN n % 5 = 0 THEN 2 ELSE 1 END,
                    'Pergunta de teste ' || n,
                    'Resposta de teste ' || n,
                    n % 7 <> 0,
                    n % 11 = 0,
                    ?,
                    CAST(? AS vector)
                FROM generate_series(1, ?) AS serie(n)
                """,
                vetorTexto,
                vetorTexto,
                TOTAL_REGISTROS
        );

        jdbc.update(
                """
                INSERT INTO resposta_perfis (resposta_id, perfil)
                SELECT id,
                    CASE WHEN id % 3 = 0
                         THEN 'GESTOR'
                         ELSE 'FUNCIONARIO'
                    END
                FROM respostas_reutilizaveis
                """
        );

        jdbc.execute("ANALYZE respostas_reutilizaveis");
        jdbc.execute("ANALYZE resposta_perfis");

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
                resultado.add(jdbc.queryForObject(
                        """
                        SELECT r.id, r.empresa_id, r.prompt_original,
                               r.resposta_gerada, r.reutilizavel,
                               r.possui_dados_sensiveis, p.perfil
                        FROM respostas_reutilizaveis r
                        JOIN resposta_perfis p ON p.resposta_id = r.id
                        WHERE r.id = ?
                        """,
                        (rs, rowNum) -> {
                            var resposta = new RespostaReutilizavel();

                            ReflectionTestUtils.setField(
                                    resposta, "id", rs.getLong("id")
                            );

                            resposta.setEmpresaId(rs.getLong("empresa_id"));
                            resposta.setPromptOriginal(
                                    rs.getString("prompt_original")
                            );
                            resposta.setRespostaGerada(
                                    rs.getString("resposta_gerada")
                            );
                            resposta.setReutilizavel(
                                    rs.getBoolean("reutilizavel")
                            );
                            resposta.setPossuiDadosSensiveis(
                                    rs.getBoolean("possui_dados_sensiveis")
                            );
                            resposta.setPerfisPermitidos(Set.of(
                                    PerfilUsuario.valueOf(rs.getString("perfil"))
                            ));

                            return resposta;
                        },
                        id
                ));
            }

            return resultado;
        });

        var embeddings = mock(EmbeddingService.class);
        when(embeddings.gerarEmbedding(anyString())).thenReturn(vetor);

        busca = new BuscaVetorialService(
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
    void deveManterFiltrosComVolumeERegistrarTempos() throws Exception {
        assertEquals(
                TOTAL_REGISTROS,
                jdbc.queryForObject(
                        "SELECT count(*) FROM respostas_reutilizaveis",
                        Integer.class
                ).intValue()
        );

        for (int i = 0; i < 5; i++) {
            consultarEValidar();
        }

        List<Double> temposMs = new ArrayList<>();

        for (int i = 0; i < AMOSTRAS; i++) {
            long inicio = System.nanoTime();
            var resultado = busca.buscarSemelhantes(
                    1L, "Pergunta de teste", 5, 0.70
            );
            double duracaoMs = (System.nanoTime() - inicio) / 1_000_000.0;

            validar(resultado);
            temposMs.add(duracaoMs);
        }

        Collections.sort(temposMs);

        double mediana = (
                temposMs.get(AMOSTRAS / 2 - 1)
                        + temposMs.get(AMOSTRAS / 2)
        ) / 2;

        double p95 = temposMs.get(
                (int) Math.ceil(AMOSTRAS * 0.95) - 1
        );

        String relatorio = String.format(
                Locale.ROOT,
                """
                SCRUM-421 — Busca vetorial com volume controlado
                Registros: %d
                Dimensoes: 1536
                Aquecimentos: 5
                Amostras: %d
                Limite de resultados: 5
                Similaridade minima: 0.70
                Mediana: %.3f ms
                P95: %.3f ms
                Maximo: %.3f ms

                Embeddings simulados e identicos.
                PostgreSQL, pgvector e filtros SQL reais.
                Hidratacao das entidades por JDBC em repositorio simulado.
                Tempos incluem busca e hidratacao; excluem chamada a OpenAI.
                Execucao sequencial, sem carga concorrente.
                Nao representa benchmark de producao nem comprova uso do indice.
                """,
                TOTAL_REGISTROS,
                AMOSTRAS,
                mediana,
                p95,
                temposMs.get(AMOSTRAS - 1)
        );

        Path destino = Path.of(
                "target", "performance", "SCRUM-421-busca-vetorial.txt"
        );

        Files.createDirectories(destino.getParent());
        Files.writeString(destino, relatorio);

        System.out.println(relatorio);
    }

    private void consultarEValidar() {
        validar(busca.buscarSemelhantes(
                1L, "Pergunta de teste", 5, 0.70
        ));
    }

    private void validar(List<RespostaReutilizavel> resultado) {
        assertEquals(5, resultado.size());

        assertEquals(
                5L,
                resultado.stream()
                        .map(RespostaReutilizavel::getId)
                        .distinct()
                        .count()
        );

        for (var resposta : resultado) {
            assertEquals(1L, resposta.getEmpresaId().longValue());
            assertTrue(resposta.getReutilizavel());
            assertFalse(resposta.getPossuiDadosSensiveis());
            assertTrue(
                    resposta.getPerfisPermitidos()
                            .contains(PerfilUsuario.FUNCIONARIO)
            );
        }
    }
}