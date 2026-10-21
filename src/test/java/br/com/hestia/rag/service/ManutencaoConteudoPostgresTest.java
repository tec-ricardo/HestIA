package br.com.hestia.rag.service;

import br.com.hestia.autenticacao.contexto.ContextoEmpresa;
import br.com.hestia.autenticacao.contexto.EmpresaAtual;
import br.com.hestia.empresa.model.Empresa;
import br.com.hestia.rag.dto.AtualizacaoConteudoDTO;
import br.com.hestia.reutilizavel.dto.RespostaReutilizavelDTO;
import br.com.hestia.usuario.model.PerfilUsuario;
import br.com.hestia.usuario.model.Usuario;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.sql.DriverManager;
import java.util.Collections;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@EnabledIfEnvironmentVariable(named = "HESTIA_PG_TEST_URL", matches = ".+")
class ManutencaoConteudoPostgresTest {

    private static final String SCHEMA =
            "manutencao_" + UUID.randomUUID().toString().replace("-", "");

    @Autowired
    private ManutencaoConteudoService service;

    @Autowired
    private EmpresaAtual empresaAtual;

    @Autowired
    private JdbcTemplate jdbc;

    private Usuario usuario;

    @DynamicPropertySource
    static void configurar(DynamicPropertyRegistry registry) {
        try (var connection = DriverManager.getConnection(
                System.getenv("HESTIA_PG_TEST_URL"),
                System.getenv("HESTIA_PG_TEST_USER"),
                System.getenv("HESTIA_PG_TEST_PASSWORD")
        ); var statement = connection.createStatement()) {
            statement.execute("CREATE SCHEMA " + SCHEMA);
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Não foi possível preparar o schema de teste.",
                    exception
            );
        }

        String url = System.getenv("HESTIA_PG_TEST_URL");
        String separador = url.contains("?") ? "&" : "?";

        registry.add(
                "spring.datasource.url",
                () -> url + separador + "currentSchema=" + SCHEMA + ",public"
        );
        registry.add(
                "spring.datasource.username",
                () -> System.getenv("HESTIA_PG_TEST_USER")
        );
        registry.add(
                "spring.datasource.password",
                () -> System.getenv("HESTIA_PG_TEST_PASSWORD")
        );
        registry.add(
                "spring.datasource.driver-class-name",
                () -> "org.postgresql.Driver"
        );
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.flyway.schemas", () -> SCHEMA);
        registry.add("spring.flyway.default-schema", () -> SCHEMA);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    }

    @BeforeEach
    void prepararUsuario() {
        RequestContextHolder.setRequestAttributes(
                new ServletRequestAttributes(new MockHttpServletRequest())
        );

        empresaAtual.definir(new ContextoEmpresa(10L, 1L));

        usuario = new Usuario();
        usuario.setId(10L);
        usuario.setAtivo(true);
        usuario.setPerfil(PerfilUsuario.FUNCIONARIO);
        usuario.setEmpresa(
                new Empresa(1L, "Empresa de teste", "12345678901234", null, null)
        );
    }

    @AfterEach
    void limparContexto() {
        RequestContextHolder.resetRequestAttributes();
    }

    @AfterAll
    static void removerSchemaDeTeste() throws Exception {
        try (var connection = DriverManager.getConnection(
                System.getenv("HESTIA_PG_TEST_URL"),
                System.getenv("HESTIA_PG_TEST_USER"),
                System.getenv("HESTIA_PG_TEST_PASSWORD")
        ); var statement = connection.createStatement()) {
            statement.execute("DROP SCHEMA IF EXISTS " + SCHEMA + " CASCADE");
        }
    }

    @Test
    void deveVersionarAtualizarERetirarPreservandoHistorico() {
        var dto = new RespostaReutilizavelDTO();
        dto.setEmpresaId(1L);
        dto.setPromptOriginal("Pergunta inicial");
        dto.setRespostaGerada("Resposta inicial");

        Long id = service.cadastrar(dto, usuario).getId();

        assertEquals(
                1,
                jdbc.queryForObject(
                        "SELECT count(*) FROM resposta_versoes WHERE resposta_id = ?",
                        Integer.class,
                        id
                )
        );

        String vetor = "["
                + String.join(",", Collections.nCopies(1536, "0.1"))
                + "]";

        jdbc.update(
                """
                UPDATE respostas_reutilizaveis
                SET embedding = ?,
                    embedding_vector = CAST(? AS vector)
                WHERE id = ?
                """,
                vetor, vetor, id
        );

        service.atualizar(
                id,
                new AtualizacaoConteudoDTO(
                        "Pergunta nova",
                        "Resposta nova",
                        "HestIA",
                        "RESPOSTA"
                ),
                usuario
        );

        assertEquals(
                java.util.List.of("Resposta inicial", "Resposta nova"),
                jdbc.queryForList(
                        """
                        SELECT resposta_gerada
                        FROM resposta_versoes
                        WHERE resposta_id = ?
                        ORDER BY numero_versao
                        """,
                        String.class,
                        id
                )
        );

        assertEquals(
                1,
                jdbc.queryForObject(
                        """
                        SELECT count(*)
                        FROM respostas_reutilizaveis
                        WHERE id = ?
                          AND embedding IS NULL
                          AND embedding_vector IS NULL
                        """,
                        Integer.class,
                        id
                )
        );

        service.retirar(id, usuario, "Conteúdo desatualizado");
        service.retirar(id, usuario, "Conteúdo desatualizado");

        assertEquals(
                Boolean.FALSE,
                jdbc.queryForObject(
                        "SELECT reutilizavel FROM respostas_reutilizaveis WHERE id = ?",
                        Boolean.class,
                        id
                )
        );

        assertEquals(
                2,
                jdbc.queryForObject(
                        "SELECT count(*) FROM resposta_versoes WHERE resposta_id = ?",
                        Integer.class,
                        id
                )
        );
    }
}