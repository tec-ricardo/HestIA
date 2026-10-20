
package br.com.hestia.rag.service;

import br.com.hestia.autenticacao.contexto.EmpresaAtual;
import br.com.hestia.usuario.model.Usuario;
import br.com.hestia.usuario.repository.UsuarioRepository;
import br.com.hestia.reutilizavel.service.AcessoRespostaService;
import br.com.hestia.reutilizavel.model.RespostaReutilizavel;
import br.com.hestia.reutilizavel.repository.RespostaReutilizavelRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class BuscaVetorialService {

    private static final int DIMENSAO = 1536;

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
            throw new IllegalArgumentException("Perfil não autorizado para indexação.");
        }

        if (!Boolean.TRUE.equals(resposta.getReutilizavel())
                || !Boolean.FALSE.equals(resposta.getPossuiDadosSensiveis())) {
            throw new IllegalArgumentException(
                    "Resposta não autorizada para indexação."
            );
        }

        String vetor = gerarVetor(resposta.getPromptOriginal());

        jdbcTemplate.update(
                """
                UPDATE respostas_reutilizaveis
                SET embedding = ?,
                    embedding_vector = CAST(? AS vector)
                WHERE id = ?
                  AND empresa_id = ?
                  AND reutilizavel = true
                  AND possui_dados_sensiveis = false
                  AND EXISTS (SELECT 1 FROM resposta_perfis p
                              WHERE p.resposta_id = respostas_reutilizaveis.id AND p.perfil = ?)
                """,
                vetor,
                vetor,
                respostaId,
                empresaId,
                usuario.getPerfil().name()
        );
    }

    @Transactional(readOnly = true)
    public List<RespostaReutilizavel> buscarSemelhantes(
            Long empresaId,
            String pergunta,
            int limite
    ) {

        Usuario usuario = usuarioAutorizado(empresaId);

        if (pergunta == null || pergunta.isBlank()) {
            throw new IllegalArgumentException(
                    "Informe uma pergunta válida."
            );
        }

        if (limite < 1 || limite > 20) {
            throw new IllegalArgumentException(
                    "O limite deve estar entre 1 e 20."
            );
        }

        String vetor = gerarVetor(pergunta);

        List<Long> ids = jdbcTemplate.queryForList(
                """
                SELECT id
                FROM respostas_reutilizaveis
                WHERE empresa_id = ?
                  AND reutilizavel = true
                  AND possui_dados_sensiveis = false
                  AND embedding_vector IS NOT NULL
                  AND EXISTS (SELECT 1 FROM resposta_perfis p
                              WHERE p.resposta_id = respostas_reutilizaveis.id AND p.perfil = ?)
                ORDER BY embedding_vector <=> CAST(? AS vector)
                LIMIT ?
                """,
                Long.class,
                empresaId,
                usuario.getPerfil().name(),
                vetor,
                limite
        );

        if (ids.isEmpty()) {
            return List.of();
        }

        Map<Long, RespostaReutilizavel> respostas =
                repository.findAllById(ids)
                        .stream()
                        .filter(r -> empresaId.equals(r.getEmpresaId()))
                        .filter(r -> r.getPerfisPermitidos().contains(usuario.getPerfil()))
                        .filter(r -> Boolean.TRUE.equals(r.getReutilizavel()))
                        .filter(r -> Boolean.FALSE.equals(
                                r.getPossuiDadosSensiveis()
                        ))
                        .collect(Collectors.toMap(
                                RespostaReutilizavel::getId,
                                Function.identity()
                        ));

        return ids.stream()
                .map(respostas::get)
                .filter(r -> r != null)
                .toList();
    }

    private Usuario usuarioAutorizado(Long empresaId) {
        empresaAtual.validar(empresaId);
        Usuario usuario = usuarios.findById(empresaAtual.obterContexto().usuarioId())
                .orElseThrow(() -> new IllegalArgumentException("Usuário autenticado inexistente."));
        acesso.validarEmpresa(usuario, empresaId);
        return usuario;
    }

    private String gerarVetor(String texto) {

        if (texto == null || texto.isBlank()) {
            throw new IllegalArgumentException(
                    "O texto não pode estar vazio."
            );
        }

        List<Float> embedding =
                embeddingService.gerarEmbedding(texto);

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
