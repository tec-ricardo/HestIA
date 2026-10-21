package br.com.hestia.rag.service;

import br.com.hestia.rag.dto.AtualizacaoConteudoDTO;
import br.com.hestia.rag.model.RespostaVersao;
import br.com.hestia.rag.repository.RespostaVersaoRepository;
import br.com.hestia.reutilizavel.dto.RespostaReutilizavelDTO;
import br.com.hestia.reutilizavel.model.RespostaReutilizavel;
import br.com.hestia.reutilizavel.service.AcessoRespostaService;
import br.com.hestia.reutilizavel.service.RespostaReutilizavelService;
import br.com.hestia.usuario.model.PerfilUsuario;
import br.com.hestia.usuario.model.Usuario;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Objects;

@Service
public class ManutencaoConteudoService {

    private final RespostaReutilizavelService respostas;
    private final RespostaVersaoRepository versoes;
    private final AcessoRespostaService acesso;
    private final EntityManager entityManager;
    private final JdbcTemplate jdbc;

    public ManutencaoConteudoService(
            RespostaReutilizavelService respostas,
            RespostaVersaoRepository versoes,
            AcessoRespostaService acesso,
            EntityManager entityManager,
            JdbcTemplate jdbc
    ) {
        this.respostas = respostas;
        this.versoes = versoes;
        this.acesso = acesso;
        this.entityManager = entityManager;
        this.jdbc = jdbc;
    }

    @Transactional
    public RespostaReutilizavel cadastrar(
            RespostaReutilizavelDTO dto,
            Usuario usuario
    ) {
        acesso.validarEmpresa(usuario, dto.getEmpresaId());

        if (dto.getEmbedding() != null && !dto.getEmbedding().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "O embedding deve ser gerado pelo processo de indexação."
            );
        }

        RespostaReutilizavel resposta = respostas.cadastrar(dto, usuario);
        entityManager.flush();

        versoes.save(new RespostaVersao(
                resposta, 1, "HestIA", "RESPOSTA"
        ));

        return resposta;
    }

    @Transactional
    public RespostaReutilizavel atualizar(
            Long id,
            AtualizacaoConteudoDTO dto,
            Usuario usuario
    ) {
        RespostaReutilizavel resposta = bloquearParaManutencao(id, usuario);

        if (!Boolean.TRUE.equals(resposta.getReutilizavel())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "O conteúdo foi retirado."
            );
        }

        if (Objects.equals(resposta.getPromptOriginal(), dto.promptOriginal())
                && Objects.equals(
                resposta.getRespostaGerada(), dto.respostaGerada()
        )) {
            return resposta;
        }

        int ultimaVersao = versoes
                .findTopByRespostaIdAndRespostaEmpresaIdOrderByNumeroVersaoDesc(
                        id, resposta.getEmpresaId()
                )
                .map(RespostaVersao::getNumeroVersao)
                .orElse(0);

        if (ultimaVersao == 0) {
            versoes.save(new RespostaVersao(
                    resposta, 1, "HestIA", "RESPOSTA"
            ));
            ultimaVersao = 1;
        }

        resposta.setPromptOriginal(dto.promptOriginal());
        resposta.setRespostaGerada(dto.respostaGerada());

        versoes.save(new RespostaVersao(
                resposta,
                Math.incrementExact(ultimaVersao),
                dto.origem(),
                dto.tipoConteudo()
        ));

        invalidarEmbedding(resposta);
        return resposta;
    }

    @Transactional
    public void retirar(Long id, Usuario usuario, String justificativa) {
        RespostaReutilizavel resposta = bloquearParaManutencao(id, usuario);
        if (justificativa == null || justificativa.isBlank() || justificativa.length() > 1000)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Justificativa de até 1000 caracteres obrigatória.");
        if (!Boolean.TRUE.equals(resposta.getReutilizavel())) return;

        boolean possuiHistorico = versoes
                .findTopByRespostaIdAndRespostaEmpresaIdOrderByNumeroVersaoDesc(
                        id, resposta.getEmpresaId()
                )
                .isPresent();

        if (!possuiHistorico) {
            versoes.save(new RespostaVersao(
                    resposta, 1, "HestIA", "RESPOSTA"
            ));
        }

        resposta.retirar(usuario.getId(), justificativa);
        invalidarEmbedding(resposta);
    }

    @Transactional(readOnly = true)
    public br.com.hestia.reutilizavel.dto.RevisaoRespostaDTO consultarRevisao(Long id, Usuario usuario) {
        var resposta = consultarParaManutencao(id, usuario, LockModeType.NONE);
        return br.com.hestia.reutilizavel.dto.RevisaoRespostaDTO.from(resposta,
                versoes.findByRespostaIdAndRespostaEmpresaIdOrderByNumeroVersaoDesc(id, resposta.getEmpresaId()));
    }

    private RespostaReutilizavel bloquearParaManutencao(Long id, Usuario usuario) {
        return consultarParaManutencao(id, usuario, LockModeType.PESSIMISTIC_WRITE);
    }

    private RespostaReutilizavel consultarParaManutencao(
            Long id,
            Usuario usuario, LockModeType bloqueio
    ) {
        Long empresaId = acesso.validarEmpresa(usuario, null);

        if (id == null || id <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Identificador inválido."
            );
        }

        RespostaReutilizavel resposta = entityManager.find(
                RespostaReutilizavel.class,
                id,
                bloqueio
        );

        if (resposta == null
                || !empresaId.equals(resposta.getEmpresaId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        boolean administrador = usuario.getPerfil() == PerfilUsuario.ADMIN;
        boolean autor = Objects.equals(
                resposta.getUsuarioId(), usuario.getId()
        );
        boolean perfilPermitido = resposta.getPerfisPermitidos()
                .contains(usuario.getPerfil());

        if (!administrador && !(autor && perfilPermitido)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Sem permissão para manter este conteúdo."
            );
        }

        return resposta;
    }

    private void invalidarEmbedding(RespostaReutilizavel resposta) {
        resposta.setEmbedding(null);
        entityManager.flush();

        jdbc.update(
                """
                UPDATE respostas_reutilizaveis
                SET embedding = NULL,
                    embedding_vector = NULL
                WHERE id = ?
                  AND empresa_id = ?
                """,
                resposta.getId(),
                resposta.getEmpresaId()
        );
    }
}