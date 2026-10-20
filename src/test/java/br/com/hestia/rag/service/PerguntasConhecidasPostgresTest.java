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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@EnabledIfEnvironmentVariable(named = "HESTIA_PG_TEST_URL", matches = ".+")
class PerguntasConhecidasPostgresTest {

    private static final String PERGUNTA_DADOS =
            "Posso enviar dados pessoais para uma IA?";
    private static final String REFORMULACAO_DADOS =
            "É permitido colocar informações pessoais em ferramentas de IA?";
    private static final String PERGUNTA_FERRAMENTAS =
            "Quais ferramentas de IA posso utilizar?";
    private static final String PERGUNTA_REVISAO =
            "Preciso revisar uma resposta gerada por IA?";

    private static final String RESPOSTA_DADOS =
            "Neste cenário de teste, dados pessoais não devem ser enviados.";
    private static final String RESPOSTA_FERRAMENTAS =
            "Neste cenário de teste, utilize somente ferramentas aprovadas.";
    private static final String RESPOSTA_REVISAO =
            "Neste cenário de teste, revise o conteúdo antes de utilizá-lo.";

    private final Map<Long, RespostaReutilizavel> respostas = new HashMap<>();

    private JdbcTemplate jdbc;
    private BuscaVetorialService busca;
    private String schema;

    @BeforeEach
    void preparar() {
        String url = System.getenv("HESTIA_PG_TEST_URL");
        schema = "perguntas_" + UUID.randomUUID()
                .toString().replace("-", "");

        var datasource = new DriverManagerDataSource(
                url + (url.contains("?") ? "&" : "?")
                        + "currentSchema=" + schema + ",public",
                System.getenv("HESTIA_PG_TEST_USER"),
                System.getenv("HESTIA_PG_TEST_PASSWORD")
        );

        jdbc = new JdbcTemplate(datasource);
        jdbc.execute("CREATE SCHEMA " + schema);

        Flyway.configure()
                .dataSource(datasource)
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
                new Empresa(1L, "Empresa de teste", "12345678901234", null, null)
        );

        var contexto = new EmpresaAtual();
        contexto.definir(new ContextoEmpresa(1L, 1L));

        var usuarios = mock(UsuarioRepository.class);
        when(usuarios.findById(1L)).thenReturn(Optional.of(usuario));

        var repository = mock(RespostaReutilizavelRepository.class);

        when(repository.findAllById(any())).thenAnswer(invocation -> {
            Iterable<Long> ids = invocation.getArgument(0);
            List<RespostaReutilizavel> encontradas = new ArrayList<>();

            for (Long id : ids) {
                if (respostas.containsKey(id)) {
                    encontradas.add(respostas.get(id));
                }
            }

            return encontradas;
        });

        var embeddings = mock(EmbeddingService.class);

        when(embeddings.gerarEmbedding(anyString()))
                .thenAnswer(invocation -> vetorPergunta(
                        invocation.getArgument(0)
                ));

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
    void deveRecuperarRespostaEsperadaParaCadaPerguntaConhecida() {
        long dados = inserir(
                PERGUNTA_DADOS, RESPOSTA_DADOS, 0,
                1L, PerfilUsuario.FUNCIONARIO, true, false
        );
        long ferramentas = inserir(
                PERGUNTA_FERRAMENTAS, RESPOSTA_FERRAMENTAS, 1,
                1L, PerfilUsuario.FUNCIONARIO, true, false
        );
        long revisao = inserir(
                PERGUNTA_REVISAO, RESPOSTA_REVISAO, 2,
                1L, PerfilUsuario.FUNCIONARIO, true, false
        );

        verificarResposta(PERGUNTA_DADOS, dados, RESPOSTA_DADOS);
        verificarResposta(
                PERGUNTA_FERRAMENTAS, ferramentas, RESPOSTA_FERRAMENTAS
        );
        verificarResposta(PERGUNTA_REVISAO, revisao, RESPOSTA_REVISAO);
    }

    @Test
    void deveRecuperarMesmaRespostaParaReformulacaoControlada() {
        long dados = inserir(
                PERGUNTA_DADOS, RESPOSTA_DADOS, 0,
                1L, PerfilUsuario.FUNCIONARIO, true, false
        );
        inserir(
                PERGUNTA_REVISAO, RESPOSTA_REVISAO, 2,
                1L, PerfilUsuario.FUNCIONARIO, true, false
        );

        verificarResposta(REFORMULACAO_DADOS, dados, RESPOSTA_DADOS);
    }

    @Test
    void naoDeveRecuperarConteudoRetiradoMesmoComVetorPresente() {
        inserir(
                PERGUNTA_DADOS, RESPOSTA_DADOS, 0,
                1L, PerfilUsuario.FUNCIONARIO, false, false
        );

        assertTrue(busca.buscarSemelhantes(1L, PERGUNTA_DADOS, 1).isEmpty());
    }

    @Test
    void naoDeveRecuperarRespostaDeOutraEmpresaPerfilOuConteudoSensivel() {
        inserir(
                PERGUNTA_DADOS, RESPOSTA_DADOS, 0,
                2L, PerfilUsuario.FUNCIONARIO, true, false
        );
        inserir(
                PERGUNTA_DADOS, RESPOSTA_DADOS, 0,
                1L, PerfilUsuario.GESTOR, true, false
        );
        inserir(
                PERGUNTA_DADOS, RESPOSTA_DADOS, 0,
                1L, PerfilUsuario.FUNCIONARIO, true, true
        );

        assertTrue(busca.buscarSemelhantes(1L, PERGUNTA_DADOS, 1).isEmpty());
    }

    @Test
    void deveRetornarVazioQuandoNaoHaConteudoNaBase() {
        assertTrue(busca.buscarSemelhantes(1L, PERGUNTA_DADOS, 1).isEmpty());
    }

    private void verificarResposta(
            String pergunta,
            long idEsperado,
            String textoEsperado
    ) {
        var resultado = busca.buscarSemelhantes(1L, pergunta, 1);

        assertEquals(1, resultado.size());
        assertEquals(idEsperado, resultado.get(0).getId().longValue());
        assertEquals(textoEsperado, resultado.get(0).getRespostaGerada());
    }

    private long inserir(
            String pergunta,
            String texto,
            int eixo,
            Long empresaId,
            PerfilUsuario perfil,
            boolean reutilizavel,
            boolean sensivel
    ) {
        String vetor = vetor(eixo).toString();

        Long id = jdbc.queryForObject(
                """
                INSERT INTO respostas_reutilizaveis (
                    empresa_id, prompt_original, resposta_gerada,
                    reutilizavel, possui_dados_sensiveis,
                    embedding, embedding_vector
                )
                VALUES (?, ?, ?, ?, ?, ?, CAST(? AS vector))
                RETURNING id
                """,
                Long.class,
                empresaId,
                pergunta,
                texto,
                reutilizavel,
                sensivel,
                vetor,
                vetor
        );

        jdbc.update(
                "INSERT INTO resposta_perfis (resposta_id, perfil) VALUES (?, ?)",
                id,
                perfil.name()
        );

        var resposta = new RespostaReutilizavel();
        ReflectionTestUtils.setField(resposta, "id", id);
        resposta.setEmpresaId(empresaId);
        resposta.setPromptOriginal(pergunta);
        resposta.setRespostaGerada(texto);
        resposta.setReutilizavel(reutilizavel);
        resposta.setPossuiDadosSensiveis(sensivel);
        resposta.setEmbedding(vetor);
        resposta.setPerfisPermitidos(Set.of(perfil));

        respostas.put(id, resposta);
        return id;
    }

    private List<Float> vetorPergunta(String pergunta) {
        if (PERGUNTA_DADOS.equals(pergunta)
                || REFORMULACAO_DADOS.equals(pergunta)) {
            return vetor(0);
        }
        if (PERGUNTA_FERRAMENTAS.equals(pergunta)) {
            return vetor(1);
        }
        if (PERGUNTA_REVISAO.equals(pergunta)) {
            return vetor(2);
        }

        throw new IllegalArgumentException(
                "Pergunta não cadastrada no cenário de teste."
        );
    }

    private List<Float> vetor(int eixo) {
        var valores = new ArrayList<>(Collections.nCopies(1536, 0f));
        valores.set(eixo, 1f);
        return valores;
    }
}