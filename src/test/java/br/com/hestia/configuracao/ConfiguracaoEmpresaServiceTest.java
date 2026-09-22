package br.com.hestia.configuracao;

import br.com.hestia.configuracao.dto.ConfiguracaoEmpresaDTO;
import br.com.hestia.configuracao.model.ConfiguracaoEmpresa;
import br.com.hestia.configuracao.repository.ConfiguracaoEmpresaRepository;
import br.com.hestia.configuracao.service.ConfiguracaoEmpresaService;
import br.com.hestia.empresa.model.Empresa;
import br.com.hestia.empresa.repository.EmpresaRepository;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ConfiguracaoEmpresaServiceTest {
    @Test
    void devePersistirConfiguracoesDaEmpresa() {
        ConfiguracaoEmpresaRepository repository = mock(ConfiguracaoEmpresaRepository.class);
        EmpresaRepository empresaRepository = mock(EmpresaRepository.class);
        Empresa empresa = new Empresa(1L, "HestIA", "12345678000199", null, null);
        ConfiguracaoEmpresa configuracao = new ConfiguracaoEmpresa(empresa);
        when(repository.findByEmpresaId(1L)).thenReturn(Optional.of(configuracao));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        var service = new ConfiguracaoEmpresaService(repository, empresaRepository);

        var resposta = service.alterar(1L, new ConfiguracaoEmpresaDTO(true, false, new BigDecimal("500.00"), 180));

        assertFalse(resposta.bloquearFerramentasNaoAprovadas());
        assertEquals(180, resposta.diasRetencaoAuditoria());
        assertEquals(new BigDecimal("500.00"), resposta.limiteCustoMensal());
        verify(repository).save(configuracao);
    }
}
