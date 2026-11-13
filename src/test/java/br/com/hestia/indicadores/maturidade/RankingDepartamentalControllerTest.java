package br.com.hestia.indicadores.maturidade;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RankingDepartamentalControllerTest {
    @Test
    void preservaEmpateEDeixaSemPosicaoQuemNaoTemScoreGlobal() {
        var linhas = RankingDepartamentalController.ordenar(List.of(
                linha(4L, "Sem dados", null),
                linha(2L, "B", "80.00"),
                linha(3L, "C", "70"),
                linha(1L, "A", "80")
        ), true);

        assertEquals(List.of(1L, 2L, 3L, 4L),
                linhas.stream().map(RankingDepartamentalController.Linha::departamentoId).toList());
        assertEquals(Arrays.asList(1, 1, 3, null),
                linhas.stream().map(RankingDepartamentalController.Linha::posicao).toList());
    }

    @Test
    void gestorNaoRecebePosicaoBaseadaApenasNoProprioDepartamento() {
        var linhas = RankingDepartamentalController.ordenar(List.of(linha(1L, "A", "80")), false);
        assertNull(linhas.getFirst().posicao());
    }


    @Test
    void funcionarioNaoConsultaDadosGerenciais() {
        validarRecusa(br.com.hestia.usuario.model.PerfilUsuario.FUNCIONARIO, false, 403);
    }

    @Test
    void gestorSemDepartamentoNaoConsultaRanking() {
        validarRecusa(br.com.hestia.usuario.model.PerfilUsuario.GESTOR, false, 403);
    }

    @Test
    void administradorNaoConsultaPeriodoInvertido() {
        validarRecusa(br.com.hestia.usuario.model.PerfilUsuario.ADMIN, true, 400);
    }

    private void validarRecusa(br.com.hestia.usuario.model.PerfilUsuario perfil,
                               boolean periodoInvertido, int status) {
        var jdbc = mock(org.springframework.jdbc.core.JdbcTemplate.class);
        var acesso = mock(br.com.hestia.reutilizavel.service.AcessoRespostaService.class);
        var maturidade = mock(ResultadoMaturidadeService.class);
        var risco = mock(ClassificacaoRiscoDepartamental.class);
        var usuario = mock(br.com.hestia.usuario.model.Usuario.class);
        when(usuario.getPerfil()).thenReturn(perfil);
        when(acesso.validarEmpresa(usuario, null)).thenReturn(1L);
        var controller = new RankingDepartamentalController(jdbc, acesso, maturidade, risco);
        var inicio = java.time.LocalDate.of(2026, 11, 3);
        var fim = periodoInvertido ? inicio.minusDays(1) : inicio.plusDays(1);
        var erro = org.junit.jupiter.api.Assertions.assertThrows(
                org.springframework.web.server.ResponseStatusException.class,
                () -> controller.consultar(usuario, inicio, fim));
        assertEquals(status, erro.getStatusCode().value());
        org.mockito.Mockito.verifyNoInteractions(jdbc, maturidade, risco);
    }

    private RankingDepartamentalController.Linha linha(Long id, String nome, String score) {
        var resultado = mock(ResultadoMaturidadeService.Resultado.class);
        when(resultado.scoreGlobal()).thenReturn(score == null ? null : new BigDecimal(score));
        return new RankingDepartamentalController.Linha(id, nome, null, resultado, null);
    }
}
