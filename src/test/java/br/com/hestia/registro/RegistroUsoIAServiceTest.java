package br.com.hestia.registro;

import br.com.hestia.empresa.model.Empresa;
import br.com.hestia.ferramenta.model.FerramentaIA;
import br.com.hestia.ferramenta.model.StatusFerramentaIA;
import br.com.hestia.ferramenta.repository.FerramentaIARepository;
import br.com.hestia.registro.dto.RegistroUsoIACriacaoDTO;
import br.com.hestia.registro.model.FinalidadeUso;
import br.com.hestia.registro.model.RegistroUsoIA;
import br.com.hestia.registro.model.StatusExecucao;
import br.com.hestia.registro.repository.RegistroUsoIARepository;
import br.com.hestia.registro.service.RegistroUsoIAService;
import br.com.hestia.usuario.model.Usuario;
import br.com.hestia.usuario.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegistroUsoIAServiceTest {

    @Mock RegistroUsoIARepository registroRepository;
    @Mock UsuarioRepository usuarioRepository;
    @Mock FerramentaIARepository ferramentaRepository;
    @Mock Usuario usuario;
    @Mock FerramentaIA ferramenta;

    private RegistroUsoIAService service;

    @BeforeEach
    void setUp() {
        service = new RegistroUsoIAService(registroRepository, usuarioRepository, ferramentaRepository);
    }

    @Test
    void deveRegistrarUsoECalcularTotalDeTokens() {
        Empresa empresa = new Empresa(10L, "HestIA", "12345678000199", null, null);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(ferramentaRepository.findById(2L)).thenReturn(Optional.of(ferramenta));
        when(usuario.getEmpresa()).thenReturn(empresa);
        when(ferramenta.getEmpresa()).thenReturn(empresa);
        when(ferramenta.getStatus()).thenReturn(StatusFerramentaIA.APROVADA);
        when(usuario.getNome()).thenReturn("Ricardo");
        when(ferramenta.getNome()).thenReturn("Assistente");
        when(registroRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var dto = new RegistroUsoIACriacaoDTO(
                1L, 2L, "modelo-x", null, FinalidadeUso.GERACAO_CODIGO,
                120, 80, new BigDecimal("0.015"), StatusExecucao.SUCESSO, 900L);

        var resposta = service.criar(dto);

        ArgumentCaptor<RegistroUsoIA> captor = ArgumentCaptor.forClass(RegistroUsoIA.class);
        verify(registroRepository).save(captor.capture());
        assertEquals(200, captor.getValue().getTokensTotal());
        assertEquals(200, resposta.tokensTotal());
        assertEquals("Ricardo", resposta.usuarioNome());
    }

    @Test
    void deveImpedirRegistroEntreEmpresasDiferentes() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(ferramentaRepository.findById(2L)).thenReturn(Optional.of(ferramenta));
        when(usuario.getEmpresa()).thenReturn(new Empresa(10L, "A", "12345678000199", null, null));
        when(ferramenta.getEmpresa()).thenReturn(new Empresa(20L, "B", "98765432000199", null, null));

        var excecao = assertThrows(IllegalArgumentException.class, () -> service.criar(dtoPadrao()));

        assertTrue(excecao.getMessage().contains("mesma empresa"));
        verifyNoInteractions(registroRepository);
    }

    @Test
    void deveImpedirUsoDeFerramentaBloqueada() {
        Empresa empresa = new Empresa(10L, "HestIA", "12345678000199", null, null);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(ferramentaRepository.findById(2L)).thenReturn(Optional.of(ferramenta));
        when(usuario.getEmpresa()).thenReturn(empresa);
        when(ferramenta.getEmpresa()).thenReturn(empresa);
        when(ferramenta.getStatus()).thenReturn(StatusFerramentaIA.BLOQUEADA);

        assertThrows(IllegalArgumentException.class, () -> service.criar(dtoPadrao()));
        verifyNoInteractions(registroRepository);
    }

    private RegistroUsoIACriacaoDTO dtoPadrao() {
        return new RegistroUsoIACriacaoDTO(
                1L, 2L, "modelo-x", null, FinalidadeUso.PESQUISA,
                10, 20, null, StatusExecucao.SUCESSO, null);
    }
}
