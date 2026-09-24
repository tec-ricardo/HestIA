package br.com.hestia.conformidade;

import br.com.hestia.conformidade.dto.AvaliacaoConformidadeDTO;
import br.com.hestia.conformidade.model.*;
import br.com.hestia.conformidade.repository.AvaliacaoConformidadeRepository;
import br.com.hestia.conformidade.service.AvaliacaoConformidadeService;
import br.com.hestia.empresa.model.Empresa;
import br.com.hestia.ferramenta.model.FerramentaIA;
import br.com.hestia.ferramenta.model.StatusFerramentaIA;
import br.com.hestia.politica.model.PoliticaUso;
import br.com.hestia.politica.repository.PoliticaUsoRepository;
import br.com.hestia.registro.model.RegistroUsoIA;
import br.com.hestia.registro.repository.RegistroUsoIARepository;
import br.com.hestia.usuario.model.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.EnumSet;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AvaliacaoConformidadeServiceTest {
    @Mock AvaliacaoConformidadeRepository avaliacaoRepository;
    @Mock RegistroUsoIARepository registroRepository;
    @Mock PoliticaUsoRepository politicaRepository;
    @Mock RegistroUsoIA registro;
    @Mock PoliticaUso politica;
    @Mock Usuario usuario;
    @Mock FerramentaIA ferramenta;

    private AvaliacaoConformidadeService service;

    @BeforeEach
    void setUp() {
        service = new AvaliacaoConformidadeService(avaliacaoRepository, registroRepository, politicaRepository);
    }

    @Test
    void deveClassificarComoConformeQuandoTodosCriteriosSaoAtendidos() {
        prepararEntidadesDaMesmaEmpresa();
        when(politica.getAtiva()).thenReturn(true);
        when(ferramenta.getStatus()).thenReturn(StatusFerramentaIA.APROVADA);
        when(registro.getFerramenta()).thenReturn(ferramenta);
        when(registro.getFinalidade()).thenReturn(br.com.hestia.registro.model.FinalidadeUso.PESQUISA);
        when(avaliacaoRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        var confirmados = EnumSet.of(CriterioConformidade.DADOS_SENSIVEIS,
                CriterioConformidade.VALIDACAO_HUMANA, CriterioConformidade.LGPD);

        var resposta = service.avaliar(new AvaliacaoConformidadeDTO(1L, 2L, confirmados, "Revisado"));

        assertEquals(StatusConformidade.CONFORME, resposta.status());
        assertEquals(7, resposta.criteriosAtendidos().size());
    }

    @Test
    void deveRejeitarPoliticaDeOutraEmpresa() {
        when(registroRepository.findById(1L)).thenReturn(Optional.of(registro));
        when(politicaRepository.findById(2L)).thenReturn(Optional.of(politica));
        when(registro.getUsuario()).thenReturn(usuario);
        when(usuario.getEmpresa()).thenReturn(new Empresa(10L, "A", "12345678000199", null, null));
        when(politica.getEmpresa()).thenReturn(new Empresa(20L, "B", "98765432000199", null, null));

        assertThrows(IllegalArgumentException.class, () -> service.avaliar(
                new AvaliacaoConformidadeDTO(1L, 2L, EnumSet.noneOf(CriterioConformidade.class), null)));
        verify(avaliacaoRepository, never()).save(any());
    }

    private void prepararEntidadesDaMesmaEmpresa() {
        Empresa empresa = new Empresa(10L, "HestIA", "12345678000199", null, null);
        when(registroRepository.findById(1L)).thenReturn(Optional.of(registro));
        when(politicaRepository.findById(2L)).thenReturn(Optional.of(politica));
        when(registro.getUsuario()).thenReturn(usuario);
        when(usuario.getEmpresa()).thenReturn(empresa);
        when(politica.getEmpresa()).thenReturn(empresa);
    }
}
