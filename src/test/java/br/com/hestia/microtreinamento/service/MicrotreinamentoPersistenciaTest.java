package br.com.hestia.microtreinamento;

import br.com.hestia.microtreinamento.model.ConclusaoMicrotreinamento;
import br.com.hestia.microtreinamento.model.HistoricoMicrotreinamento;
import br.com.hestia.microtreinamento.model.NecessidadeMicrotreinamento;
import br.com.hestia.microtreinamento.model.TemaTreinamento;
import br.com.hestia.microtreinamento.model.TipoErro;
import br.com.hestia.microtreinamento.repository.ConclusaoMicrotreinamentoRepository;
import br.com.hestia.microtreinamento.repository.HistoricoMicrotreinamentoRepository;
import br.com.hestia.microtreinamento.repository.NecessidadeMicrotreinamentoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
class MicrotreinamentoPersistenciaTest {

    @Autowired
    private HistoricoMicrotreinamentoRepository historicoRepository;

    @Autowired
    private ConclusaoMicrotreinamentoRepository conclusaoRepository;

    @Autowired
    private NecessidadeMicrotreinamentoRepository necessidadeRepository;

    @Test
    void devePersistirHistoricoEConclusao() {

        HistoricoMicrotreinamento historico =
                new HistoricoMicrotreinamento(1L);

        ConclusaoMicrotreinamento conclusao =
                new ConclusaoMicrotreinamento(
                        1L,
                        10L,
                        LocalDateTime.now(),
                        true
                );

        historico.adicionarConclusao(conclusao);

        HistoricoMicrotreinamento salvo =
                historicoRepository.saveAndFlush(historico);

        assertNotNull(salvo.getId());

        var conclusaoSalva =
                conclusaoRepository
                        .findByUsuarioIdAndMicrotreinamentoId(
                                1L,
                                10L
                        );

        assertTrue(conclusaoSalva.isPresent());
        assertTrue(conclusaoSalva.get().isConcluido());
        assertNotNull(
                conclusaoSalva.get().getDataConclusao()
        );
    }

    @Test
    void devePersistirNecessidadeDeMicrotreinamento() {

        NecessidadeMicrotreinamento necessidade =
                new NecessidadeMicrotreinamento(
                        1L,
                        TipoErro.DADOS_SENSIVEIS,
                        TemaTreinamento.PROTECAO_DE_DADOS,
                        LocalDateTime.now(),
                        true
                );

        NecessidadeMicrotreinamento salva =
                necessidadeRepository.saveAndFlush(
                        necessidade
                );

        assertNotNull(salva.getId());

        NecessidadeMicrotreinamento encontrada =
                necessidadeRepository
                        .findById(salva.getId())
                        .orElseThrow();

        assertEquals(
                TipoErro.DADOS_SENSIVEIS,
                encontrada.getTipoErro()
        );

        assertEquals(
                TemaTreinamento.PROTECAO_DE_DADOS,
                encontrada.getTemaTreinamento()
        );

        assertTrue(encontrada.isNecessario());
    }
}