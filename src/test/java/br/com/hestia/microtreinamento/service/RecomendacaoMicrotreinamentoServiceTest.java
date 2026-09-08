package br.com.hestia.microtreinamento.service;

import br.com.hestia.microtreinamento.model.Microtreinamento;
import br.com.hestia.microtreinamento.model.NecessidadeMicrotreinamento;
import br.com.hestia.microtreinamento.model.TipoErro;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class RecomendacaoMicrotreinamentoServiceTest {

    private RecomendacaoMicrotreinamentoService service;

    @BeforeEach
    void setUp() {
        service = new RecomendacaoMicrotreinamentoService();
    }

    @Test
    void deveRecomendarTreinamentoDeProtecaoDeDados() {

        NecessidadeMicrotreinamento necessidade =
                criarNecessidade(
                        TipoErro.DADOS_SENSIVEIS,
                        true
                );

        Microtreinamento treinamento =
                service.recomendar(necessidade);

        assertNotNull(treinamento);
        assertEquals(
                "Proteção de Dados em IA",
                treinamento.getTitulo()
        );
    }

    @Test
    void deveRecomendarTreinamentoDeLgpd() {

        NecessidadeMicrotreinamento necessidade =
                criarNecessidade(
                        TipoErro.VIOLACAO_LGPD,
                        true
                );

        Microtreinamento treinamento =
                service.recomendar(necessidade);

        assertNotNull(treinamento);
        assertEquals(
                "LGPD Aplicada ao Uso de Inteligência Artificial",
                treinamento.getTitulo()
        );
    }

    @Test
    void deveRecomendarTreinamentoDeValidacaoHumana() {

        NecessidadeMicrotreinamento necessidade =
                criarNecessidade(
                        TipoErro.AUSENCIA_VALIDACAO_HUMANA,
                        true
                );

        Microtreinamento treinamento =
                service.recomendar(necessidade);

        assertNotNull(treinamento);
        assertEquals(
                "Validação Humana de Respostas de IA",
                treinamento.getTitulo()
        );
    }

    @Test
    void naoDeveRecomendarTreinamentoQuandoNaoForNecessario() {

        NecessidadeMicrotreinamento necessidade =
                criarNecessidade(
                        TipoErro.DADOS_SENSIVEIS,
                        false
                );

        Microtreinamento treinamento =
                service.recomendar(necessidade);

        assertNull(treinamento);
    }

    @Test
    void deveRelacionarTodosOsTiposDeErroAUmTreinamento() {

        for (TipoErro tipoErro : TipoErro.values()) {

            Microtreinamento treinamento =
                    service.buscarTreinamentoPorErro(tipoErro);

            assertNotNull(
                    treinamento,
                    "Não foi encontrado treinamento para: " + tipoErro
            );
        }
    }

    private NecessidadeMicrotreinamento criarNecessidade(
            TipoErro tipoErro,
            boolean necessario
    ) {

        return new NecessidadeMicrotreinamento(
                1L,
                tipoErro,
                tipoErro.getTemaTreinamento(),
                LocalDateTime.now(),
                necessario
        );
    }
}