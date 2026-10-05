package br.com.hestia.politica.service;

import br.com.hestia.auditoria.service.AuditoriaService;
import br.com.hestia.empresa.model.Empresa;
import br.com.hestia.empresa.repository.EmpresaRepository;
import br.com.hestia.politica.dto.PoliticaUsoDTO;
import br.com.hestia.politica.model.PoliticaUso;
import br.com.hestia.politica.repository.PoliticaUsoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PoliticaUsoServiceTest {

    private PoliticaUsoRepository politicaRepository;
    private EmpresaRepository empresaRepository;
    private AuditoriaService auditoriaService;
    private PoliticaUsoService service;

    @BeforeEach
    void setUp() {
        politicaRepository = mock(PoliticaUsoRepository.class);
        empresaRepository = mock(EmpresaRepository.class);
        auditoriaService = mock(AuditoriaService.class);

        service = new PoliticaUsoService(
                politicaRepository,
                empresaRepository,
                auditoriaService
        );
    }

    @Test
    void deveCriarPolitica() {
        Empresa empresa = new Empresa();
        empresa.setId(1L);

        PoliticaUsoDTO dto = criarDTO();

        when(empresaRepository.findById(1L))
                .thenReturn(Optional.of(empresa));

        when(politicaRepository.save(any(PoliticaUso.class)))
                .thenAnswer(invocation -> {
                    PoliticaUso politica = invocation.getArgument(0);
                    politica.setId(10L);
                    return politica;
                });

        PoliticaUso resultado = service.criar(dto);

        assertThat(resultado.getId()).isEqualTo(10L);
        assertThat(resultado.getTitulo()).isEqualTo("Uso de IA");
        assertThat(resultado.getEmpresa()).isEqualTo(empresa);
        assertThat(resultado.getAtiva()).isTrue();

        verify(politicaRepository).save(any(PoliticaUso.class));
        verify(auditoriaService)
                .registrarOperacao("POST", "/politicas-uso/10", 200);
    }

    @Test
    void deveFalharAoCriarPoliticaComEmpresaInexistente() {
        PoliticaUsoDTO dto = criarDTO();

        when(empresaRepository.findById(1L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.criar(dto))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Empresa não encontrada.");

        verify(politicaRepository, never())
                .save(any(PoliticaUso.class));
    }

    @Test
    void deveListarTodasPoliticas() {
        PoliticaUso politica = new PoliticaUso();

        when(politicaRepository.findAll())
                .thenReturn(List.of(politica));

        List<PoliticaUso> resultado = service.listarTodas();

        assertThat(resultado).hasSize(1);
        verify(politicaRepository).findAll();
    }

    @Test
    void deveBuscarPoliticaPorId() {
        PoliticaUso politica = new PoliticaUso();
        politica.setId(5L);

        when(politicaRepository.findById(5L))
                .thenReturn(Optional.of(politica));

        PoliticaUso resultado = service.buscarPorId(5L);

        assertThat(resultado.getId()).isEqualTo(5L);
    }

    @Test
    void deveFalharQuandoPoliticaNaoExiste() {
        when(politicaRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscarPorId(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Política de uso não encontrada.");
    }

    @Test
    void deveListarPoliticasPorEmpresa() {
        when(politicaRepository.findByEmpresaId(1L))
                .thenReturn(List.of(new PoliticaUso()));

        List<PoliticaUso> resultado =
                service.listarPorEmpresa(1L);

        assertThat(resultado).hasSize(1);

        verify(politicaRepository)
                .findByEmpresaId(1L);
    }

    @Test
    void deveAtualizarPolitica() {
        PoliticaUso politica = new PoliticaUso();
        politica.setId(10L);

        Empresa empresa = new Empresa();
        empresa.setId(1L);

        PoliticaUsoDTO dto = criarDTO();
        dto.setTitulo("Política atualizada");

        when(politicaRepository.findById(10L))
                .thenReturn(Optional.of(politica));

        when(empresaRepository.findById(1L))
                .thenReturn(Optional.of(empresa));

        when(politicaRepository.save(politica))
                .thenReturn(politica);

        PoliticaUso resultado =
                service.atualizar(10L, dto);

        assertThat(resultado.getTitulo())
                .isEqualTo("Política atualizada");

        assertThat(resultado.getEmpresa())
                .isEqualTo(empresa);

        verify(auditoriaService)
                .registrarOperacao("PUT", "/politicas-uso/10", 200);
    }

    @Test
    void deveExcluirPolitica() {
        PoliticaUso politica = new PoliticaUso();
        politica.setId(10L);

        when(politicaRepository.findById(10L))
                .thenReturn(Optional.of(politica));

        service.excluir(10L);

        verify(politicaRepository).delete(politica);

        verify(auditoriaService)
                .registrarOperacao("DELETE", "/politicas-uso/10", 200);
    }

    private PoliticaUsoDTO criarDTO() {
        PoliticaUsoDTO dto = new PoliticaUsoDTO();

        dto.setTitulo("Uso de IA");
        dto.setDescricao("Política para uso de IA");
        dto.setConteudo("Conteúdo da política");
        dto.setVersao("1.0");
        dto.setAtiva(true);
        dto.setEmpresaId(1L);

        return dto;
    }
}