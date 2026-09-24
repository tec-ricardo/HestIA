package br.com.hestia.auditoria.repository;

import br.com.hestia.auditoria.model.LogAuditoria;
import br.com.hestia.auditoria.model.ResultadoAuditoria;
import br.com.hestia.auditoria.model.TipoAcaoAuditoria;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class LogAuditoriaRepositoryTest {

    @Autowired
    private LogAuditoriaRepository repository;

    @Test
    void devePersistirEFiltrarLogsPorUsuarioEmOrdemDecrescente() {
        var antigo = new LogAuditoria(
                7L,
                TipoAcaoAuditoria.CRIAR,
                "EMPRESA",
                1L,
                "Primeiro evento",
                ResultadoAuditoria.SUCESSO,
                null,
                "estado 1"
        );
        antigo.setDataHora(LocalDateTime.now().minusMinutes(5));

        var recente = new LogAuditoria(
                7L,
                TipoAcaoAuditoria.ALTERAR,
                "EMPRESA",
                1L,
                "Segundo evento",
                ResultadoAuditoria.SUCESSO,
                "estado 1",
                "estado 2"
        );
        recente.setDataHora(LocalDateTime.now());

        repository.saveAndFlush(antigo);
        repository.saveAndFlush(recente);

        var logs = repository.findByUsuarioIdOrderByDataHoraDesc(7L);

        assertThat(logs).hasSize(2);
        assertThat(logs.get(0).getDescricao()).isEqualTo("Segundo evento");
        assertThat(logs.get(1).getDescricao()).isEqualTo("Primeiro evento");
    }
}
