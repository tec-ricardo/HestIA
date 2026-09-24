package br.com.hestia.gamificacao;

import br.com.hestia.gamificacao.model.*;
import br.com.hestia.usuario.model.Usuario;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProgressaoExperienciaTest {
    @Test
    void deveEvoluirAutomaticamenteConformeXpAcumulado() {
        ProgressoUsuario progresso = new ProgressoUsuario(mock(Usuario.class));
        progresso.adicionarXp(99);
        assertEquals(NivelExperiencia.APRENDIZ, progresso.getNivel());
        progresso.adicionarXp(1);
        assertEquals(NivelExperiencia.CONSCIENTE, progresso.getNivel());
        progresso.adicionarXp(600);
        assertEquals(NivelExperiencia.EMBAIXADOR, progresso.getNivel());
    }

    @Test
    void deveConcluirMissaoSomenteAoAtingirMeta() {
        Missao missao = mock(Missao.class);
        when(missao.getMetaQuantidade()).thenReturn(2);
        ProgressoMissao progresso = new ProgressoMissao(mock(Usuario.class), missao);
        assertFalse(progresso.incrementar());
        assertTrue(progresso.incrementar());
        assertNotNull(progresso.getConcluidaEm());
        assertFalse(progresso.incrementar());
    }

    @Test
    void deveDefinirXpDasAcoesDoMvp() {
        assertEquals(10, AcaoExperiencia.REGISTRO_USO.getPontos());
        assertEquals(25, AcaoExperiencia.USO_CONFORME.getPontos());
        assertEquals(15, AcaoExperiencia.REUTILIZACAO_RESPOSTA.getPontos());
    }
}
