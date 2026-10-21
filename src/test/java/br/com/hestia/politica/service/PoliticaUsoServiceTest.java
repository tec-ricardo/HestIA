package br.com.hestia.politica.service;

import br.com.hestia.auditoria.service.AuditoriaService;
import br.com.hestia.autenticacao.contexto.*;
import br.com.hestia.empresa.model.Empresa;
import br.com.hestia.empresa.repository.EmpresaRepository;
import br.com.hestia.politica.dto.PoliticaUsoDTO;
import br.com.hestia.politica.model.*;
import br.com.hestia.politica.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PoliticaUsoServiceTest {
    private final PoliticaUsoRepository politicas = mock(PoliticaUsoRepository.class);
    private final EmpresaRepository empresas = mock(EmpresaRepository.class);
    private final AuditoriaService auditoria = mock(AuditoriaService.class);
    private final PoliticaUsoVersaoRepository versoes = mock(PoliticaUsoVersaoRepository.class);
    private final EmpresaAtual contexto = new EmpresaAtual();
    private PoliticaUsoService service;
    private Empresa empresa;

    @BeforeEach
    void preparar() {
        contexto.definir(new ContextoEmpresa(7L, 1L));
        empresa = new Empresa(1L, "Empresa de teste", "00000000000000", null, null);
        service = new PoliticaUsoService(politicas, empresas, auditoria, versoes, contexto);
    }

    @Test
    void deveCriarPoliticaComPrimeiraRevisaoEAutorAutenticado() {
        when(empresas.findById(1L)).thenReturn(Optional.of(empresa));
        when(politicas.save(any())).thenAnswer(i -> {
            PoliticaUso p = i.getArgument(0);
            p.setId(10L);
            return p;
        });
        var p = service.criar(dto());
        assertThat(p.getRevisao()).isEqualTo(1);
        verify(versoes).save(argThat(v -> v.getPoliticaUsoId().equals(10L)
                && v.getAutorId().equals(7L) && v.getEmpresaId().equals(1L)
                && v.getRevisao() == 1 && v.getConteudo().equals("Conteudo original")
                && v.getOrigem().equals("CRIACAO")));
        verify(auditoria).registrarOperacao("POST", "/politicas/10", 200);
    }

    @Test
    void deveRecusarEmpresaInexistente() {
        assertThatThrownBy(() -> service.criar(dto())).isInstanceOf(NoSuchElementException.class);
        verifyNoInteractions(versoes);
    }

    @Test
    void deveListarSomentePoliticasNaoExcluidasDaEmpresaAutenticada() {
        var ativa = politica();
        var excluida = politica();
        excluida.excluirLogicamente();
        when(politicas.findByEmpresaId(1L)).thenReturn(List.of(ativa, excluida));
        assertThat(service.listarTodas()).containsExactly(ativa);
        verify(politicas, never()).findAll();
        assertThatThrownBy(() -> service.listarPorEmpresa(2L)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void devePreservarVersaoAnteriorMesmoComMesmoRotuloDeVersao() {
        var p = politica();
        var anterior = new PoliticaUsoVersao(p, 7L, "CRIACAO");
        when(politicas.buscarBloqueada(10L)).thenReturn(Optional.of(p));
        var nova = dto();
        nova.setConteudo("Conteudo atualizado");
        service.atualizar(10L, nova);
        assertThat(p.getRevisao()).isEqualTo(2);
        assertThat(p.getVersao()).isEqualTo("1.0");
        assertThat(anterior.getConteudo()).isEqualTo("Conteudo original");
        assertThat(anterior.getRevisao()).isEqualTo(1);
        verify(versoes).save(argThat(v -> v.getRevisao() == 2
                && v.getConteudo().equals("Conteudo atualizado")
                && v.getOrigem().equals("ATUALIZACAO")));
    }

    @Test
    void deveExcluirLogicamenteSemApagarHistoricoENemDuplicarExclusao() {
        var p = politica();
        when(politicas.buscarBloqueada(10L)).thenReturn(Optional.of(p));
        when(politicas.findById(10L)).thenReturn(Optional.of(p));
        service.excluir(10L);
        service.excluir(10L);
        assertThat(p.isExcluida()).isTrue();
        assertThat(p.getAtiva()).isFalse();
        assertThat(p.getRevisao()).isEqualTo(2);
        verify(politicas, never()).delete(any());
        verify(versoes, times(1)).save(argThat(v -> v.isExcluida() && v.getOrigem().equals("EXCLUSAO")));
        assertThatThrownBy(() -> service.buscarPorId(10L)).isInstanceOf(NoSuchElementException.class);
        assertThatThrownBy(() -> service.atualizar(10L, dto())).isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void devePermitirConsultarHistoricoDePoliticaExcluida() {
        var p = politica();
        var v = new PoliticaUsoVersao(p, 7L, "CRIACAO");
        p.excluirLogicamente();
        when(politicas.findById(10L)).thenReturn(Optional.of(p));
        when(versoes.findByPoliticaUsoIdOrderByRevisaoDesc(10L)).thenReturn(List.of(v));
        when(versoes.findByPoliticaUsoIdAndRevisao(10L, 1)).thenReturn(Optional.of(v));
        assertThat(service.listarVersoes(10L)).hasSize(1);
        assertThat(service.buscarVersao(10L, 1).conteudo()).isEqualTo("Conteudo original");
    }

    @Test
    void deveBloquearConsultaEAlteracaoDeOutraEmpresa() {
        var p = politica();
        p.setEmpresa(new Empresa(2L, "Outra", "00000000000002", null, null));
        when(politicas.findById(10L)).thenReturn(Optional.of(p));
        when(politicas.buscarBloqueada(10L)).thenReturn(Optional.of(p));
        assertThatThrownBy(() -> service.buscarPorId(10L)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.listarVersoes(10L)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.buscarVersao(10L, 1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.atualizar(10L, dto())).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.excluir(10L)).isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(versoes);
    }

    @Test
    void deveRecusarTransferenciaEntreEmpresas() {
        when(politicas.buscarBloqueada(10L)).thenReturn(Optional.of(politica()));
        var d = dto();
        d.setEmpresaId(2L);
        assertThatThrownBy(() -> service.atualizar(10L, d)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.criar(d)).isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(versoes);
    }

    @Test
    void deveRetornarAusenteQuandoPoliticaOuRevisaoNaoExistir() {
        assertThatThrownBy(() -> service.buscarPorId(99L)).isInstanceOf(NoSuchElementException.class);
        when(politicas.findById(10L)).thenReturn(Optional.of(politica()));
        assertThatThrownBy(() -> service.buscarVersao(10L, 9)).isInstanceOf(NoSuchElementException.class);
    }

    private PoliticaUso politica() {
        var p = new PoliticaUso();
        p.setId(10L); p.setEmpresa(empresa); p.setTitulo("Uso de IA");
        p.setDescricao("Descricao"); p.setConteudo("Conteudo original"); p.setVersao("1.0");
        return p;
    }

    private PoliticaUsoDTO dto() {
        var d = new PoliticaUsoDTO();
        d.setEmpresaId(1L); d.setTitulo("Uso de IA"); d.setDescricao("Descricao");
        d.setConteudo("Conteudo original"); d.setVersao("1.0"); d.setAtiva(true);
        return d;
    }
}
