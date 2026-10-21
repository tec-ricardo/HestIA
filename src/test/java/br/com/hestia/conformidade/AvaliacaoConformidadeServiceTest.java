package br.com.hestia.conformidade;

import br.com.hestia.autenticacao.contexto.*;
import br.com.hestia.conformidade.dto.*;
import br.com.hestia.conformidade.model.*;
import br.com.hestia.conformidade.repository.AvaliacaoConformidadeRepository;
import br.com.hestia.conformidade.service.AvaliacaoConformidadeService;
import br.com.hestia.empresa.model.Empresa;
import br.com.hestia.ferramenta.model.*;
import br.com.hestia.politica.model.*;
import br.com.hestia.politica.repository.*;
import br.com.hestia.registro.model.*;
import br.com.hestia.registro.repository.RegistroUsoIARepository;
import br.com.hestia.usuario.model.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AvaliacaoConformidadeServiceTest {
    private final AvaliacaoConformidadeRepository avaliacoes = mock(AvaliacaoConformidadeRepository.class);
    private final RegistroUsoIARepository registros = mock(RegistroUsoIARepository.class);
    private final PoliticaUsoRepository politicas = mock(PoliticaUsoRepository.class);
    private final PoliticaUsoVersaoRepository versoes = mock(PoliticaUsoVersaoRepository.class);
    private final RegistroUsoIA registro = mock(RegistroUsoIA.class);
    private final FerramentaIA ferramenta = mock(FerramentaIA.class);
    private final EmpresaAtual contexto = new EmpresaAtual();
    private PoliticaUso politica;
    private AvaliacaoConformidadeService service;

    @BeforeEach
    void preparar() {
        contexto.definir(new ContextoEmpresa(7L, 10L));
        var empresa = new Empresa(10L, "Teste", "00000000000000", null, null);
        var usuario = new Usuario();
        usuario.setEmpresa(empresa);
        politica = new PoliticaUso();
        politica.setId(2L); politica.setEmpresa(empresa); politica.setVersao("1.0");
        politica.setTitulo("Original"); politica.setDescricao("Descricao"); politica.setConteudo("Original");
        when(registro.getUsuario()).thenReturn(usuario);
        when(registro.getId()).thenReturn(1L);
        when(registro.getFerramenta()).thenReturn(ferramenta);
        when(registro.getFinalidade()).thenReturn(FinalidadeUso.PESQUISA);
        when(ferramenta.getStatus()).thenReturn(StatusFerramentaIA.APROVADA);
        when(registros.findById(1L)).thenReturn(Optional.of(registro));
        when(politicas.buscarBloqueada(2L)).thenReturn(Optional.of(politica));
        when(versoes.findByPoliticaUsoIdAndRevisao(2L, 1))
                .thenReturn(Optional.of(new PoliticaUsoVersao(politica, 7L, "CRIACAO")));
        when(avaliacoes.save(any())).thenAnswer(i -> i.getArgument(0));
        service = new AvaliacaoConformidadeService(avaliacoes, registros, politicas, versoes, contexto);
    }

    @Test
    void deveVincularResultadoARevisaoERegraPreservadas() {
        var resposta = service.avaliar(dto(EnumSet.of(CriterioConformidade.DADOS_SENSIVEIS,
                CriterioConformidade.VALIDACAO_HUMANA, CriterioConformidade.LGPD)));
        assertEquals(StatusConformidade.CONFORME, resposta.status());
        assertEquals(7, resposta.criteriosAtendidos().size());
        assertTrue(resposta.fundamentosPreservados());
        assertEquals(1, resposta.politicaRevisao());
        assertEquals("1.0", resposta.politicaVersao());
        assertEquals(AvaliacaoConformidadeService.REGRA_CODIGO, resposta.regraCodigo());
        assertEquals(AvaliacaoConformidadeService.REGRA_VERSAO, resposta.regraVersao());
        assertTrue(resposta.regraAplicada().contains("CONFORME"));
        assertTrue(resposta.limitacoes().contains("revisão humana"));
    }

    @Test
    void naoDeveAceitarConfirmacaoManualDeCriteriosAutomaticos() {
        politica.setAtiva(false);
        when(ferramenta.getStatus()).thenReturn(StatusFerramentaIA.EM_ANALISE);
        var resposta = service.avaliar(dto(EnumSet.allOf(CriterioConformidade.class)));
        assertEquals(StatusConformidade.NAO_CONFORME, resposta.status());
        assertFalse(resposta.criteriosAtendidos().contains(CriterioConformidade.POLITICA_INTERNA));
        assertFalse(resposta.criteriosAtendidos().contains(CriterioConformidade.IA_AUTORIZADA));
    }

    @Test
    void deveRejeitarPoliticaDeOutraEmpresaOuExcluida() {
        politica.setEmpresa(new Empresa(20L, "Outra", "00000000000002", null, null));
        assertThrows(IllegalArgumentException.class, () -> service.avaliar(dto(Set.of())));
        politica.setEmpresa(registro.getUsuario().getEmpresa());
        politica.excluirLogicamente();
        assertThrows(IllegalArgumentException.class, () -> service.avaliar(dto(Set.of())));
        verify(avaliacoes, never()).save(any());
    }

    @Test
    void deveRecusarAvaliacaoSemRevisaoRegistrada() {
        when(versoes.findByPoliticaUsoIdAndRevisao(2L, 1)).thenReturn(Optional.empty());
        assertThrows(IllegalStateException.class, () -> service.avaliar(dto(Set.of())));
        verify(avaliacoes, never()).save(any());
    }

    @Test
    void deveSinalizarLegadoSemInventarPoliticaOuFundamentos() {
        var legado = new AvaliacaoConformidade(registro, politica, StatusConformidade.CONFORME,
                EnumSet.allOf(CriterioConformidade.class), "Antes da migracao");
        when(avaliacoes.findById(8L)).thenReturn(Optional.of(legado));
        var resposta = service.buscar(8L);
        assertFalse(resposta.fundamentosPreservados());
        assertNull(resposta.politicaVersaoId());
        assertNull(resposta.motivo());
        assertNull(resposta.regraAplicada());
        assertTrue(resposta.limitacoes().contains("legada"));
        assertThrows(NoSuchElementException.class, () -> service.buscarPoliticaHistorica(8L));
    }

    @Test
    void deveBloquearLeituraDeAvaliacaoDeOutraEmpresa() {
        var a = new AvaliacaoConformidade(registro, politica, StatusConformidade.CONFORME, Set.of(), null);
        when(avaliacoes.findById(8L)).thenReturn(Optional.of(a));
        contexto.definir(new ContextoEmpresa(9L, 20L));
        assertThrows(IllegalArgumentException.class, () -> service.buscar(8L));
        assertThrows(IllegalArgumentException.class, () -> service.buscarPoliticaHistorica(8L));
    }

    private AvaliacaoConformidadeDTO dto(Set<CriterioConformidade> criterios) {
        return new AvaliacaoConformidadeDTO(1L, 2L, criterios, "Revisado");
    }
}
