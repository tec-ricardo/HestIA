package br.com.hestia.microtreinamento.service;

import br.com.hestia.aprendizagem.AcessoCapacitacao;
import br.com.hestia.microtreinamento.controller.EvolucaoMicrotreinamentoController;
import br.com.hestia.microtreinamento.dto.OcorrenciaErroDTO;
import br.com.hestia.microtreinamento.model.ConclusaoMicrotreinamento;
import br.com.hestia.microtreinamento.model.TipoErro;
import br.com.hestia.microtreinamento.repository.ConclusaoMicrotreinamentoRepository;
import br.com.hestia.usuario.model.Usuario;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class EvolucaoMicrotreinamentoServiceTest {

    private final LocalDateTime conclusaoEm = LocalDateTime.of(2025, 1, 15, 12, 0);
    private final TipoErro tipo = TipoErro.FERRAMENTA_NAO_AUTORIZADA;
    private JdbcTemplate jdbc;
    private Usuario usuario;
    private AcessoCapacitacao acesso;
    private ConsultaEfetividadeCapacitacaoService efetividade;
    private EvolucaoMicrotreinamentoService service;

    @BeforeEach
    void preparar() {
        var datasource = new DriverManagerDataSource(
                "jdbc:h2:mem:evolucao_" + UUID.randomUUID()
                        + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1", "sa", ""
        );
        jdbc = new JdbcTemplate(datasource);
        jdbc.execute("""
                CREATE TABLE aprendizagem_catalogo (
                    id BIGINT PRIMARY KEY, empresa_id BIGINT, treinamento_id BIGINT,
                    versao INT, conteudo_sha256 VARCHAR(64))
                """);
        jdbc.execute("""
                CREATE TABLE aprendizagem_ciclos (
                    id BIGINT PRIMARY KEY, empresa_id BIGINT, usuario_id BIGINT, catalogo_id BIGINT)
                """);
        jdbc.execute("""
                CREATE TABLE aprendizagem_tentativas (
                    id BIGINT PRIMARY KEY, ciclo_id BIGINT, nota NUMERIC(5,2), status VARCHAR(20),
                    aberta_em TIMESTAMP, finalizada_em TIMESTAMP)
                """);
        jdbc.execute("""
                CREATE TABLE aprendizagem_leituras (
                    empresa_id BIGINT, usuario_id BIGINT, treinamento_id BIGINT,
                    conclusao_id BIGINT, conteudo_sha256 VARCHAR(64), concluida_em TIMESTAMP)
                """);
        usuario = new Usuario();
        usuario.setId(10L);
        acesso = mock(AcessoCapacitacao.class);
        when(acesso.empresa(usuario)).thenReturn(1L);
        when(acesso.usuario(usuario, 10L)).thenReturn(usuario);
        var conclusoes = mock(ConclusaoMicrotreinamentoRepository.class);
        when(conclusoes.findById(100L)).thenReturn(Optional.of(
                new ConclusaoMicrotreinamento(10L, 2L, conclusaoEm, true)
        ));
        efetividade = mock(ConsultaEfetividadeCapacitacaoService.class);
        when(efetividade.consultar(usuario, 100L, tipo))
                .thenReturn(comportamento(conclusaoEm.plusDays(40)));
        service = new EvolucaoMicrotreinamentoService(conclusoes, acesso, efetividade, jdbc);
        catalogo(1, 1, 10, 1, "a");
        leitura(100, "a");
    }

    @AfterEach
    void encerrar() {
        jdbc.execute("DROP ALL OBJECTS");
        jdbc.execute("SET DB_CLOSE_DELAY 0");
    }

    @ParameterizedTest
    @CsvSource({"80,MELHORIA,40", "40,ESTABILIDADE,0", "20,PIORA,-20"})
    void deveCompararNotasEPreservarRecorrencia(int notaFinal, String situacao, int variacao) {
        tentativa(1, 1, 40, conclusaoEm.plusDays(1), "REPROVADA");
        tentativa(2, 1, notaFinal, conclusaoEm.plusDays(2),
                notaFinal >= 80 ? "APROVADA" : "REPROVADA");
        var resultado = consultar();
        var aprendizagem = resultado.aprendizagem().getFirst();
        assertEquals(situacao, aprendizagem.situacao());
        assertEquals(0, BigDecimal.valueOf(variacao).compareTo(aprendizagem.variacaoPontos()));
        assertEquals("COMPARACAO_DISPONIVEL", resultado.situacaoAprendizagem());
        assertEquals(EfetividadeCapacitacaoService.Situacao.MELHORIA,
                resultado.recorrencia().indicador().situacao());
        assertEquals(2, aprendizagem.tentativas().size());
        assertTrue(resultado.periodoPosteriorCompleto());
    }

    @Test
    void deveInformarInsuficienciaComUmaOuNenhumaTentativa() {
        assertEquals("DADOS_INSUFICIENTES", consultar().situacaoAprendizagem());
        tentativa(1, 1, 80, conclusaoEm.plusDays(1), "APROVADA");
        var resultado = consultar();
        assertEquals("DADOS_INSUFICIENTES", resultado.situacaoAprendizagem());
        assertNull(resultado.aprendizagem().getFirst().variacaoPontos());
        assertEquals(0, new BigDecimal("80").compareTo(
                resultado.aprendizagem().getFirst().tentativas().getFirst().nota()));
    }

    @Test
    void naoDeveMisturarVersoesMesmoQuandoOConteudoTemOMesmoHash() {
        catalogo(2, 1, 10, 2, "a");
        tentativa(1, 1, 40, conclusaoEm.plusDays(1), "REPROVADA");
        tentativa(2, 2, 80, conclusaoEm.plusDays(2), "APROVADA");
        var resultado = consultar();
        assertEquals(2, resultado.aprendizagem().size());
        assertTrue(resultado.aprendizagem().stream().allMatch(item -> item.variacaoPontos() == null));
        assertEquals("DADOS_INSUFICIENTES", resultado.situacaoAprendizagem());
    }

    @Test
    void deveExigirLeituraCompativelVinculadaAConclusao() {
        jdbc.update("DELETE FROM aprendizagem_leituras");
        leitura(999, "a");
        tentativa(1, 1, 80, conclusaoEm.plusDays(1), "APROVADA");
        assertTrue(consultar().aprendizagem().isEmpty());
        leitura(100, "b");
        assertTrue(consultar().aprendizagem().isEmpty());
        leitura(100, "a");
        leitura(100, "a");
        assertEquals(1, consultar().aprendizagem().getFirst().tentativas().size());
    }

    @Test
    void deveExcluirOutrasEmpresasEUsuarios() {
        catalogo(2, 2, 10, 1, "a");
        catalogo(3, 1, 20, 1, "a");
        tentativa(1, 2, 80, conclusaoEm.plusDays(1), "APROVADA");
        tentativa(2, 3, 80, conclusaoEm.plusDays(1), "APROVADA");
        assertTrue(consultar().aprendizagem().isEmpty());
    }

    @Test
    void deveExcluirTentativasIncompletasAnterioresEAposJanela() {
        tentativa(1, 1, 80, conclusaoEm.minusDays(1), "APROVADA");
        tentativa(2, 1, 80, conclusaoEm.plusDays(30), "APROVADA");
        tentativa(3, 1, 80, conclusaoEm.plusDays(40), "APROVADA");
        tentativa(4, 1, 80, conclusaoEm.plusDays(1), "EM_ANDAMENTO");
        tentativa(5, 1, 80, conclusaoEm.plusDays(1), "APROVADA");
        jdbc.update("UPDATE aprendizagem_tentativas SET nota = NULL WHERE id = 5");
        assertTrue(consultar().aprendizagem().isEmpty());
    }

    @Test
    void deveRespeitarCorteDaObservacaoSemDeclararJanelaCompleta() {
        when(efetividade.consultar(usuario, 100L, tipo))
                .thenReturn(comportamento(conclusaoEm.plusDays(5)));
        tentativa(1, 1, 40, conclusaoEm.plusDays(1), "REPROVADA");
        tentativa(2, 1, 80, conclusaoEm.plusDays(6), "APROVADA");
        var resultado = consultar();
        assertFalse(resultado.periodoPosteriorCompleto());
        assertEquals(1, resultado.aprendizagem().getFirst().tentativas().size());
        assertNull(resultado.aprendizagem().getFirst().variacaoPontos());
        assertEquals(EfetividadeCapacitacaoService.Situacao.DADOS_INSUFICIENTES,
                resultado.recorrencia().indicador().situacao());
    }

    @Test
    void deveAutorizarParticipanteAntesDeConsultarSeusResultados() {
        when(acesso.usuario(usuario, 10L)).thenThrow(
                new ResponseStatusException(HttpStatus.FORBIDDEN, "Departamento não autorizado")
        );
        assertThrows(ResponseStatusException.class, this::consultar);
        verifyNoInteractions(efetividade);
    }

    @Test
    void deveExporEvolucaoNaRotaDaConclusao() throws Exception {
        var mvc = MockMvcBuilders.standaloneSetup(new EvolucaoMicrotreinamentoController(service)).build();
        mvc.perform(get("/microtreinamentos/conclusoes/100/evolucao")
                        .requestAttr("usuarioAutenticado", usuario)
                        .param("tipoErro", tipo.name()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.empresaId").value(1))
                .andExpect(jsonPath("$.usuarioId").value(10))
                .andExpect(jsonPath("$.situacaoAprendizagem").value("DADOS_INSUFICIENTES"))
                .andExpect(jsonPath("$.recorrencia.indicador.situacao").value("MELHORIA"));
    }

    private EvolucaoMicrotreinamentoService.Resultado consultar() {
        return service.consultar(usuario, 100L, tipo);
    }

    private void catalogo(long id, long empresa, long participante, int versao, String hash) {
        jdbc.update("INSERT INTO aprendizagem_catalogo VALUES (?, ?, 2, ?, ?)", id, empresa, versao, hash);
        jdbc.update("INSERT INTO aprendizagem_ciclos VALUES (?, ?, ?, ?)", id, empresa, participante, id);
    }

    private void leitura(long conclusao, String hash) {
        jdbc.update("INSERT INTO aprendizagem_leituras VALUES (1, 10, 2, ?, ?, ?)",
                conclusao, hash, Timestamp.valueOf(conclusaoEm));
    }

    private void tentativa(long id, long ciclo, int nota, LocalDateTime data, String status) {
        jdbc.update("INSERT INTO aprendizagem_tentativas VALUES (?, ?, ?, ?, ?, ?)",
                id, ciclo, nota, status, Timestamp.valueOf(data.minusMinutes(1)), Timestamp.valueOf(data));
    }

    private ConsultaEfetividadeCapacitacaoService.Resultado comportamento(LocalDateTime observadoAte) {
        var ocorrencias = List.of(
                new OcorrenciaErroDTO("a", 1L, 10L, tipo, conclusaoEm.minusDays(3)),
                new OcorrenciaErroDTO("b", 1L, 10L, tipo, conclusaoEm.minusDays(2)),
                new OcorrenciaErroDTO("c", 1L, 10L, tipo, conclusaoEm.minusDays(1)),
                new OcorrenciaErroDTO("d", 1L, 10L, tipo, conclusaoEm.plusDays(1))
        );
        var recorrencia = new RecorrenciaErroService().calcular(
                1L, 10L, tipo, conclusaoEm, observadoAte, ocorrencias
        );
        var acompanhamento = new RecorrenciaCapacitacaoService.Resultado(100L, 2L, "Capacitação", recorrencia);
        var indicador = new EfetividadeCapacitacaoService().avaliar(acompanhamento, true);
        return new ConsultaEfetividadeCapacitacaoService.Resultado(indicador,
                new ConsultaEfetividadeCapacitacaoService.Cobertura(
                        "AVALIACOES_DE_CONFORMIDADE_DOS_REGISTROS_CADASTRADOS", true, true,
                        new FonteRecorrenciaConformidade.Janela(3, 3),
                        new FonteRecorrenciaConformidade.Janela(2, 2), "Escopo cadastrado"
                ));
    }
}
