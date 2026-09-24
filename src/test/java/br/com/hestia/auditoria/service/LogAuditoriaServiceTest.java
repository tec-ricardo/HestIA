package br.com.hestia.auditoria.service;

import br.com.hestia.auditoria.dto.LogAuditoriaCriacaoDTO;
import br.com.hestia.auditoria.model.LogAuditoria;
import br.com.hestia.auditoria.model.ResultadoAuditoria;
import br.com.hestia.auditoria.model.TipoAcaoAuditoria;
import br.com.hestia.auditoria.repository.LogAuditoriaRepository;
import org.junit.jupiter.api.Test;

import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class LogAuditoriaServiceTest {

    @Test
    void deveRegistrarLogDeAuditoria() {
        var repository = mock(LogAuditoriaRepository.class);
        var service = new LogAuditoriaService(repository);

        var dto = new LogAuditoriaCriacaoDTO(
                1L,
                TipoAcaoAuditoria.CRIAR,
                "EMPRESA",
                10L,
                "Empresa cadastrada",
                ResultadoAuditoria.SUCESSO,
                null,
                "novo estado"
        );

        when(repository.save(any(LogAuditoria.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var resultado = service.registrar(dto);

        assertThat(resultado.getUsuarioId()).isEqualTo(1L);
        assertThat(resultado.getAcao()).isEqualTo(TipoAcaoAuditoria.CRIAR);
        assertThat(resultado.getEntidade()).isEqualTo("EMPRESA");
        assertThat(resultado.getResultado()).isEqualTo(ResultadoAuditoria.SUCESSO);
        assertThat(resultado.getDataHora()).isNotNull();

        verify(repository).save(any(LogAuditoria.class));
    }

    @Test
    void deveBuscarLogPorId() {
        var repository = mock(LogAuditoriaRepository.class);
        var service = new LogAuditoriaService(repository);

        var log = new LogAuditoria(
                1L,
                TipoAcaoAuditoria.ALTERAR,
                "USUARIO",
                5L,
                "Usuário alterado",
                ResultadoAuditoria.SUCESSO,
                "antes",
                "depois"
        );

        when(repository.findById(5L)).thenReturn(Optional.of(log));

        var resultado = service.buscarPorId(5L);

        assertThat(resultado).isSameAs(log);
        verify(repository).findById(5L);
    }

    @Test
    void deveGerarErroQuandoLogNaoExistir() {
        var repository = mock(LogAuditoriaRepository.class);
        var service = new LogAuditoriaService(repository);

        when(repository.findById(999L)).thenReturn(Optional.empty());

        var exception = assertThrows(
                NoSuchElementException.class,
                () -> service.buscarPorId(999L)
        );

        assertThat(exception.getMessage())
                .isEqualTo("Log de auditoria não encontrado.");
    }
}
