package br.com.hestia.rag.repository;

import br.com.hestia.rag.model.RespostaVersao;
import br.com.hestia.reutilizavel.model.RespostaReutilizavel;
import br.com.hestia.reutilizavel.repository.RespostaReutilizavelRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class RespostaVersaoRepositoryTest {

    @Autowired
    private RespostaVersaoRepository versoes;

    @Autowired
    private RespostaReutilizavelRepository respostas;

    @Autowired
    private EntityManager entityManager;

    @Test
    void devePreservarConteudoAnteriorEOrdenarHistorico() {
        var resposta = criarResposta(1L, "Pergunta", "Resposta inicial");

        var primeira = versoes.saveAndFlush(
                new RespostaVersao(resposta, 1, "HestIA", "RESPOSTA")
        );

        String hashAnterior = primeira.getConteudoSha256();

        resposta.setRespostaGerada("Resposta atualizada");
        respostas.saveAndFlush(resposta);

        versoes.saveAndFlush(
                new RespostaVersao(resposta, 2, "HestIA", "RESPOSTA")
        );

        Long respostaId = resposta.getId();
        entityManager.clear();

        var historico = versoes
                .findByRespostaIdAndRespostaEmpresaIdOrderByNumeroVersaoDesc(
                        respostaId, 1L
                );

        assertEquals(2, historico.size());
        assertEquals(2, historico.get(0).getNumeroVersao());
        assertEquals(1, historico.get(1).getNumeroVersao());
        assertEquals("Resposta atualizada", historico.get(0).getRespostaGerada());
        assertEquals("Resposta inicial", historico.get(1).getRespostaGerada());
        assertEquals(hashAnterior, historico.get(1).getConteudoSha256());
        assertNotEquals(
                historico.get(0).getConteudoSha256(),
                historico.get(1).getConteudoSha256()
        );
        assertEquals("HestIA", historico.get(1).getOrigem());
        assertEquals("RESPOSTA", historico.get(1).getTipoConteudo());
        assertNotNull(historico.get(1).getCriadaEm());
    }

    @Test
    void deveGerarHashConsistenteSemConfundirLimitesDosTextos() {
        var resposta = criarResposta(1L, "ab", "c");

        var primeira = new RespostaVersao(
                resposta, 1, "HestIA", "RESPOSTA"
        );
        var segunda = new RespostaVersao(
                resposta, 2, "Outra origem", "RESPOSTA"
        );

        assertEquals(
                primeira.getConteudoSha256(),
                segunda.getConteudoSha256()
        );
        assertTrue(primeira.getConteudoSha256().matches("[0-9a-f]{64}"));

        resposta.setPromptOriginal("a");
        resposta.setRespostaGerada("bc");

        var terceira = new RespostaVersao(
                resposta, 3, "HestIA", "RESPOSTA"
        );

        assertNotEquals(
                primeira.getConteudoSha256(),
                terceira.getConteudoSha256()
        );
    }

    @Test
    void deveRejeitarVersaoInvalidaEMetadadosAusentes() {
        var resposta = criarResposta(1L, "Pergunta", "Resposta");

        assertThrows(
                IllegalArgumentException.class,
                () -> new RespostaVersao(
                        resposta, 0, "HestIA", "RESPOSTA"
                )
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> new RespostaVersao(
                        resposta, 1, " ", "RESPOSTA"
                )
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> new RespostaVersao(
                        resposta, 1, "HestIA", null
                )
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> new RespostaVersao(
                        new RespostaReutilizavel(), 1, "HestIA", "RESPOSTA"
                )
        );
    }

    @Test
    void deveBloquearNumeroDuplicadoParaMesmaResposta() {
        var resposta = criarResposta(1L, "Pergunta", "Resposta");

        versoes.saveAndFlush(
                new RespostaVersao(resposta, 1, "HestIA", "RESPOSTA")
        );

        assertThrows(
                DataIntegrityViolationException.class,
                () -> versoes.saveAndFlush(
                        new RespostaVersao(
                                resposta, 1, "HestIA", "RESPOSTA"
                        )
                )
        );
    }

    @Test
    void deveFiltrarHistoricoEUltimaVersaoPorEmpresa() {
        var resposta = criarResposta(1L, "Pergunta", "Resposta");

        versoes.saveAndFlush(
                new RespostaVersao(resposta, 1, "HestIA", "RESPOSTA")
        );
        versoes.saveAndFlush(
                new RespostaVersao(resposta, 2, "HestIA", "RESPOSTA")
        );

        assertTrue(
                versoes
                        .findByRespostaIdAndRespostaEmpresaIdOrderByNumeroVersaoDesc(
                                resposta.getId(), 2L
                        )
                        .isEmpty()
        );
        assertTrue(
                versoes
                        .findTopByRespostaIdAndRespostaEmpresaIdOrderByNumeroVersaoDesc(
                                resposta.getId(), 2L
                        )
                        .isEmpty()
        );
        assertEquals(
                2,
                versoes
                        .findTopByRespostaIdAndRespostaEmpresaIdOrderByNumeroVersaoDesc(
                                resposta.getId(), 1L
                        )
                        .orElseThrow()
                        .getNumeroVersao()
        );
    }

    private RespostaReutilizavel criarResposta(
            Long empresaId,
            String prompt,
            String texto
    ) {
        var resposta = new RespostaReutilizavel();
        resposta.setEmpresaId(empresaId);
        resposta.setPromptOriginal(prompt);
        resposta.setRespostaGerada(texto);

        return respostas.saveAndFlush(resposta);
    }
}