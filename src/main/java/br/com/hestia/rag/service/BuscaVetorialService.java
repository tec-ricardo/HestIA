package br.com.hestia.rag.service;

import br.com.hestia.autenticacao.contexto.EmpresaAtual;
import br.com.hestia.reutilizavel.model.RespostaReutilizavel;
import br.com.hestia.reutilizavel.repository.RespostaReutilizavelRepository;
import br.com.hestia.reutilizavel.service.AcessoRespostaService;
import br.com.hestia.usuario.model.Usuario;
import br.com.hestia.usuario.repository.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class BuscaVetorialService {
    @org.springframework.beans.factory.annotation.Autowired(required=false)
    private br.com.hestia.auditoria.service.DecisaoRecuperacaoService decisoes;

    private static final int DIMENSAO = 1536;
    @org.springframework.beans.factory.annotation.Value("${hestia.rag.similaridade-minima:0.70}")
    private double similaridadeConfigurada = 0.70;

    public double similaridadePadrao() {
        return similaridadeConfigurada;
    }

    private final JdbcTemplate jdbcTemplate;
    private final EmbeddingService embeddingService;
    private final RespostaReutilizavelRepository repository;
    private final EmpresaAtual empresaAtual;
    private final UsuarioRepository usuarios;
    private final AcessoRespostaService acesso;

    public BuscaVetorialService(
            JdbcTemplate jdbcTemplate,
            EmbeddingService embeddingService,
            RespostaReutilizavelRepository repository,
            EmpresaAtual empresaAtual,
            UsuarioRepository usuarios,
            AcessoRespostaService acesso
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.embeddingService = embeddingService;
        this.repository = repository;
        this.empresaAtual = empresaAtual;
        this.usuarios = usuarios;
        this.acesso = acesso;
    }

    @Transactional
    public void indexarResposta(Long empresaId, Long respostaId) {
        Usuario usuario = usuarioAutorizado(empresaId);

        RespostaReutilizavel resposta = repository.findById(respostaId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Resposta não encontrada."
                ));

        if (!empresaId.equals(resposta.getEmpresaId())) {
            throw new IllegalArgumentException(
                    "Resposta pertence a outra empresa."
            );
        }

        if (!resposta.getPerfisPermitidos().contains(usuario.getPerfil())) {
            throw new IllegalArgumentException(
                    "Perfil não autorizado para indexação."
            );
        }

        if (!resposta.isElegivel() || !Boolean.TRUE.equals(resposta.getReutilizavel())
                || !Boolean.FALSE.equals(resposta.getPossuiDadosSensiveis())) {
            throw new IllegalArgumentException(
                    "Resposta não autorizada para indexação."
            );
        }

        String promptIndexado = resposta.getPromptOriginal();
        String respostaIndexada = resposta.getRespostaGerada();
        String perfilAutorizado = usuario.getPerfil().name();
        String textoFonte = promptIndexado + "\n" + respostaIndexada;
        String vetor = gerarVetor(textoFonte);
        var trechos = TrechosDocumentoRag.dividir(respostaIndexada);
        var vetoresTrechos = trechos.stream().map(t -> gerarVetor(promptIndexado + "\n" + t)).toList();

        int atualizadas = jdbcTemplate.update(
                """
                UPDATE respostas_reutilizaveis
                SET embedding = ?,
                    embedding_vector = CAST(? AS vector)
                WHERE id = ?
                  AND empresa_id = ?
                  AND prompt_original = ?
                  AND resposta_gerada = ?
                  AND reutilizavel = true
                  AND status_qualidade = 'APROVADA'
                  AND (expira_em IS NULL OR expira_em > ?)
                  AND possui_dados_sensiveis = false
                  AND EXISTS (
                      SELECT 1
                      FROM resposta_perfis p
                      WHERE p.resposta_id = respostas_reutilizaveis.id
                        AND p.perfil = ?
                  )
                """,
                vetor,
                vetor,
                respostaId,
                empresaId,
                promptIndexado,
                respostaIndexada,
                java.time.LocalDateTime.now(),
                perfilAutorizado
        );

        if (atualizadas != 1) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "O conteúdo ou suas permissões mudaram durante a indexação. "
                            + "Execute a indexação novamente."
            );
        }
        jdbcTemplate.update("DELETE FROM resposta_trechos_vetoriais WHERE resposta_id = ?", respostaId);
        for (int i = 0; i < trechos.size(); i++) {
            jdbcTemplate.update("INSERT INTO resposta_trechos_vetoriais(resposta_id,ordem,texto_fonte,trecho,embedding_vector) VALUES(?,?,?,?,CAST(? AS vector))",
                    respostaId, i, textoFonte, trechos.get(i), vetoresTrechos.get(i));
        }
    }

    @Transactional(readOnly = true)
    public List<RespostaReutilizavel> buscarSemelhantes(
            Long empresaId,
            String pergunta,
            int limite
    ) {
        return buscarSemelhantes(
                empresaId, pergunta, limite, similaridadeConfigurada
        );
    }

    @Transactional(readOnly = true)
    public List<RespostaReutilizavel> buscarSemelhantes(
            Long empresaId,
            String pergunta,
            int limite,
            double similaridadeMinima
    ) {
        Usuario usuario = usuarioAutorizado(empresaId);

        if (pergunta == null || pergunta.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Informe uma pergunta válida."
            );
        }

        if (limite < 1 || limite > 20) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "O limite deve estar entre 1 e 20."
            );
        }

        if (!Double.isFinite(similaridadeMinima)
                || similaridadeMinima < 0
                || similaridadeMinima > 1) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "A similaridade mínima deve estar entre zero e um."
            );
        }

        // Não chamar a OpenAI se não existe nenhum vetor autorizado para pesquisar.
        // Sem fontes, o serviço deve retornar lista vazia, não erro 503.
        Long quantidade = jdbcTemplate.queryForObject(
                """
                SELECT count(*) FROM respostas_reutilizaveis r
                WHERE r.empresa_id = ?
                  AND r.reutilizavel = true
                  AND r.status_qualidade = 'APROVADA'
                  AND (r.expira_em IS NULL OR r.expira_em > ?)
                  AND r.possui_dados_sensiveis = false
                  AND r.embedding_vector IS NOT NULL
                  AND EXISTS (
                      SELECT 1 FROM resposta_perfis p
                      WHERE p.resposta_id = r.id AND p.perfil = ?
                  )
                """,
                Long.class,
                empresaId,
                java.time.LocalDateTime.now(),
                usuario.getPerfil().name()
        );
        if (quantidade == null || quantidade == 0) {
            if (decisoes != null) decisoes.registrar(usuario, null,
                    "RECUPERACAO_VETORIAL", true, "SEM_CONTEUDO_AUTORIZADO");
            return List.of();
        }

        String vetor = gerarVetor(pergunta);

        List<Map<String, Object>> resultados = jdbcTemplate.queryForList(
                """
                SELECT r.id, MAX(1.0 - (v.embedding_vector <=> CAST(? AS vector))) AS similaridade
                FROM respostas_reutilizaveis r
                CROSS JOIN LATERAL (
                    SELECT r.embedding_vector
                    UNION ALL
                    SELECT CAST(c.embedding_vector AS vector) FROM resposta_trechos_vetoriais c
                    WHERE c.resposta_id = r.id
                      AND c.texto_fonte = r.prompt_original || chr(10) || r.resposta_gerada
                ) v
                WHERE r.empresa_id = ?
                  AND r.reutilizavel = true
                  AND r.status_qualidade = 'APROVADA'
                  AND (r.expira_em IS NULL OR r.expira_em > ?)
                  AND r.possui_dados_sensiveis = false
                  AND r.embedding_vector IS NOT NULL
                  AND EXISTS (
                      SELECT 1 FROM resposta_perfis p
                      WHERE p.resposta_id = r.id AND p.perfil = ?
                  )
                  AND (v.embedding_vector <=> CAST(? AS vector)) <= ?
                GROUP BY r.id
                ORDER BY similaridade DESC, r.id
                LIMIT ?
                """,
                vetor,
                empresaId,
                java.time.LocalDateTime.now(),
                usuario.getPerfil().name(),
                vetor,
                1.0 - similaridadeMinima,
                limite
        );

        if (resultados.isEmpty()) {
            if(decisoes!=null)decisoes.registrar(usuario,null,"RECUPERACAO_VETORIAL",true,"SEM_CONTEUDO_AUTORIZADO");
            return List.of();
        }

        List<Long> ids = resultados.stream()
                .map(linha -> ((Number) linha.get("id")).longValue())
                .toList();

        Map<Long, Double> similaridades = resultados.stream()
                .collect(Collectors.toMap(
                        linha -> ((Number) linha.get("id")).longValue(),
                        linha -> ((Number) linha.get("similaridade")).doubleValue()
                ));

        Map<Long, RespostaReutilizavel> respostas =
                repository.findAllById(ids)
                        .stream()
                        .filter(r -> empresaId.equals(r.getEmpresaId()))
                        .filter(r -> r.getPerfisPermitidos()
                                .contains(usuario.getPerfil()))
                        .filter(r -> Boolean.TRUE.equals(r.getReutilizavel()))
                        .filter(RespostaReutilizavel::isElegivel)
                        .filter(r -> Boolean.FALSE.equals(
                                r.getPossuiDadosSensiveis()
                        ))
                        .collect(Collectors.toMap(
                                RespostaReutilizavel::getId,
                                Function.identity()
                        ));

        if(decisoes!=null)decisoes.registrar(usuario,null,"RECUPERACAO_VETORIAL",true,"FILTROS_ANTES_DO_LIMITE");
        return ids.stream()
                .map(respostas::get)
                .filter(Objects::nonNull)
                .peek(r -> r.setSimilaridade(similaridades.get(r.getId())))
                .toList();
    }

    private Usuario usuarioAutorizado(Long empresaId) {
        empresaAtual.validar(empresaId);

        Usuario usuario = usuarios
                .findById(empresaAtual.obterContexto().usuarioId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Usuário autenticado inexistente."
                ));

        acesso.validarEmpresa(usuario, empresaId);
        return usuario;
    }

    private String gerarVetor(String texto) {
        if (texto == null || texto.isBlank()) {
            throw new IllegalArgumentException(
                    "O texto não pode estar vazio."
            );
        }

        List<Float> embedding = embeddingService.gerarEmbedding(texto);

        if (embedding == null || embedding.size() != DIMENSAO) {
            throw new IllegalStateException(
                    "Dimensão do embedding inválida."
            );
        }

        StringBuilder vetor = new StringBuilder("[");

        for (int i = 0; i < embedding.size(); i++) {
            Float valor = embedding.get(i);

            if (valor == null || !Float.isFinite(valor)) {
                throw new IllegalStateException(
                        "Embedding contém valor inválido."
                );
            }

            if (i > 0) {
                vetor.append(",");
            }

            vetor.append(valor);
        }

        return vetor.append("]").toString();
    }
}