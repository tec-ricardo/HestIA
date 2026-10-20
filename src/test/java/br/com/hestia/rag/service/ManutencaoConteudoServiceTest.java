package br.com.hestia.rag.service;

import br.com.hestia.empresa.model.Empresa;
import br.com.hestia.rag.dto.AtualizacaoConteudoDTO;
import br.com.hestia.rag.model.RespostaVersao;
import br.com.hestia.rag.repository.RespostaVersaoRepository;
import br.com.hestia.reutilizavel.dto.RespostaReutilizavelDTO;
import br.com.hestia.reutilizavel.model.RespostaReutilizavel;
import br.com.hestia.reutilizavel.repository.RespostaReutilizavelRepository;
import br.com.hestia.reutilizavel.service.AcessoRespostaService;
import br.com.hestia.reutilizavel.service.RespostaReutilizavelService;
import br.com.hestia.usuario.model.PerfilUsuario;
import br.com.hestia.usuario.model.Usuario;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ManutencaoConteudoServiceTest {

    private RespostaReutilizavelService respostas;
    private RespostaVersaoRepository versoes;
    private EntityManager entityManager;
    private JdbcTemplate jdbc;
    private ManutencaoConteudoService service;
    private Usuario usuario;
    private RespostaReutilizavel resposta;

    @BeforeEach
    void preparar() {
        respostas = mock(RespostaReutilizavelService.class);
        versoes = mock(RespostaVersaoRepository.class);
        entityManager = mock(EntityManager.class);
        jdbc = mock(JdbcTemplate.class);

        var acesso = new AcessoRespostaService(
                mock(RespostaReutilizavelRepository.class)
        );

        service = new ManutencaoConteudoService(
                respostas, versoes, acesso, entityManager, jdbc
        );

        usuario = new Usuario();
        usuario.setId(10L);
        usuario.setAtivo(true);
        usuario.setPerfil(PerfilUsuario.FUNCIONARIO);
        usuario.setEmpresa(
                new Empresa(1L, "Empresa", "12345678901234", null, null)
        );

        resposta = new RespostaReutilizavel();
        ReflectionTestUtils.setField(resposta, "id", 20L);
        resposta.setEmpresaId(1L);
        resposta.setUsuarioId(10L);
        resposta.setPromptOriginal("Pergunta inicial");
        resposta.setRespostaGerada("Resposta inicial");
        resposta.setEmbedding("[1,0]");
        resposta.setPerfisPermitidos(Set.of(PerfilUsuario.FUNCIONARIO));

        when(entityManager.find(
                RespostaReutilizavel.class,
                20L,
                LockModeType.PESSIMISTIC_WRITE
        )).thenReturn(resposta);

        when(versoes
                .findTopByRespostaIdAndRespostaEmpresaIdOrderByNumeroVersaoDesc(
                        20L, 1L
                ))
                .thenReturn(Optional.empty());
    }

    @Test
    void cadastroDeveRegistrarPrimeiraVersao() {
        var dto = new RespostaReutilizavelDTO();
        dto.setEmpresaId(1L);
        dto.setPromptOriginal("Pergunta inicial");
        dto.setRespostaGerada("Resposta inicial");

        when(respostas.cadastrar(dto, usuario)).thenReturn(resposta);

        assertSame(resposta, service.cadastrar(dto, usuario));

        var captor = ArgumentCaptor.forClass(RespostaVersao.class);
        verify(versoes).save(captor.capture());

        assertEquals(1, captor.getValue().getNumeroVersao());
        assertEquals("Resposta inicial", captor.getValue().getRespostaGerada());
    }

    @Test
    void cadastroDeveRejeitarEmbeddingEnviadoPeloCliente() {
        var dto = new RespostaReutilizavelDTO();
        dto.setEmpresaId(1L);
        dto.setEmbedding("[1,0]");

        assertThrows(
                ResponseStatusException.class,
                () -> service.cadastrar(dto, usuario)
        );

        verifyNoInteractions(respostas, versoes);
    }

    @Test
    void atualizacaoDeLegadoDevePreservarTextoAnteriorEInvalidarVetor() {
        service.atualizar(20L, atualizacao(), usuario);

        var captor = ArgumentCaptor.forClass(RespostaVersao.class);
        verify(versoes, times(2)).save(captor.capture());

        var historico = captor.getAllValues();

        assertEquals(1, historico.get(0).getNumeroVersao());
        assertEquals("Resposta inicial", historico.get(0).getRespostaGerada());
        assertEquals(2, historico.get(1).getNumeroVersao());
        assertEquals("Resposta nova", historico.get(1).getRespostaGerada());
        assertEquals("Resposta nova", resposta.getRespostaGerada());
        assertNull(resposta.getEmbedding());

        verificarInvalidacao();
    }

    @Test
    void atualizacaoDeveContinuarNumeracaoExistente() {
        var anterior = new RespostaVersao(
                resposta, 3, "HestIA", "RESPOSTA"
        );

        when(versoes
                .findTopByRespostaIdAndRespostaEmpresaIdOrderByNumeroVersaoDesc(
                        20L, 1L
                ))
                .thenReturn(Optional.of(anterior));

        service.atualizar(20L, atualizacao(), usuario);

        var captor = ArgumentCaptor.forClass(RespostaVersao.class);
        verify(versoes).save(captor.capture());

        assertEquals(4, captor.getValue().getNumeroVersao());
        assertEquals("Resposta inicial", anterior.getRespostaGerada());
    }

    @Test
    void textoIgualNaoDeveCriarVersaoNemInvalidarVetor() {
        var dto = new AtualizacaoConteudoDTO(
                "Pergunta inicial",
                "Resposta inicial",
                "HestIA",
                "RESPOSTA"
        );

        service.atualizar(20L, dto, usuario);

        verifyNoInteractions(versoes, jdbc);
        assertEquals("[1,0]", resposta.getEmbedding());
    }

    @Test
    void retiradaDevePreservarTextoEHistorico() {
        service.retirar(20L, usuario);

        assertFalse(resposta.getReutilizavel());
        assertEquals("Resposta inicial", resposta.getRespostaGerada());
        assertNull(resposta.getEmbedding());

        verify(versoes).save(any(RespostaVersao.class));
        verificarInvalidacao();

        clearInvocations(versoes, jdbc);

        service.retirar(20L, usuario);

        verifyNoInteractions(versoes, jdbc);
    }

    @Test
    void conteudoRetiradoNaoDeveSerAtualizado() {
        resposta.setReutilizavel(false);

        var erro = assertThrows(
                ResponseStatusException.class,
                () -> service.atualizar(20L, atualizacao(), usuario)
        );

        assertEquals(409, erro.getStatusCode().value());
        verifyNoInteractions(versoes, jdbc);
    }

    @Test
    void outraEmpresaNaoPodeAtualizarNemRetirar() {
        resposta.setEmpresaId(2L);

        var atualizacao = assertThrows(
                ResponseStatusException.class,
                () -> service.atualizar(20L, atualizacao(), usuario)
        );

        var retirada = assertThrows(
                ResponseStatusException.class,
                () -> service.retirar(20L, usuario)
        );

        assertEquals(404, atualizacao.getStatusCode().value());
        assertEquals(404, retirada.getStatusCode().value());
        verifyNoInteractions(versoes, jdbc);
    }

    @Test
    void outroAutorOuPerfilRevogadoNaoPodeAlterar() {
        resposta.setUsuarioId(99L);

        assertThrows(
                ResponseStatusException.class,
                () -> service.atualizar(20L, atualizacao(), usuario)
        );

        resposta.setUsuarioId(usuario.getId());
        resposta.setPerfisPermitidos(Set.of());

        assertThrows(
                ResponseStatusException.class,
                () -> service.retirar(20L, usuario)
        );

        verifyNoInteractions(versoes, jdbc);
    }

    @Test
    void adminDaMesmaEmpresaPodeRetirarConteudoDeOutroAutor() {
        usuario.setPerfil(PerfilUsuario.ADMIN);
        resposta.setUsuarioId(99L);
        resposta.setPerfisPermitidos(Set.of());

        service.retirar(20L, usuario);

        assertFalse(resposta.getReutilizavel());
        verificarInvalidacao();
    }

    @Test
    void usuarioInativoDeveSerBloqueadoAntesDeAcessarConteudo() {
        usuario.setAtivo(false);

        assertThrows(
                ResponseStatusException.class,
                () -> service.atualizar(20L, atualizacao(), usuario)
        );

        verifyNoInteractions(entityManager, versoes, jdbc);
    }

    private AtualizacaoConteudoDTO atualizacao() {
        return new AtualizacaoConteudoDTO(
                "Pergunta nova",
                "Resposta nova",
                "HestIA",
                "RESPOSTA"
        );
    }

    private void verificarInvalidacao() {
        verify(jdbc).update(
                argThat((String sql) ->
                        sql.contains("embedding_vector = NULL")
                                && sql.contains("empresa_id = ?")
                ),
                eq(20L),
                eq(1L)
        );
    }
}