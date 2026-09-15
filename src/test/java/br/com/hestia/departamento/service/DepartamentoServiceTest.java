package br.com.hestia.departamento.service;

import br.com.hestia.departamento.dto.DepartamentoDTO;
import br.com.hestia.departamento.model.Departamento;
import br.com.hestia.departamento.repository.DepartamentoRepository;
import br.com.hestia.empresa.model.Empresa;
import br.com.hestia.empresa.repository.EmpresaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DepartamentoServiceTest {

    private static final Long EMPRESA_ID = 10L;

    @Mock
    private DepartamentoRepository departamentoRepository;

    @Mock
    private EmpresaRepository empresaRepository;

    @InjectMocks
    private DepartamentoService departamentoService;

    @Test
    void deveCadastrarDepartamentoQuandoDadosForemValidos() {
        // Arrange
        Empresa empresa = novaEmpresa();
        DepartamentoDTO dto = novoDepartamento("Tecnologia");
        when(empresaRepository.findById(EMPRESA_ID)).thenReturn(Optional.of(empresa));
        when(departamentoRepository.save(any(Departamento.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Departamento salvo = departamentoService.cadastrar(dto);

        // Assert
        assertEquals("Tecnologia", salvo.getNome());
        assertEquals("Ricardo", salvo.getResponsavel());
        assertEquals("Diretoria > Tecnologia", salvo.getEstruturaHierarquica());
        assertSame(empresa, salvo.getEmpresa());
        verify(departamentoRepository).save(any(Departamento.class));
    }

    @Test
    void deveRejeitarCadastroQuandoNomeEstiverVazio() {
        // Arrange
        DepartamentoDTO dto = novoDepartamento("   ");

        // Act
        IllegalArgumentException erro = assertThrows(
                IllegalArgumentException.class,
                () -> departamentoService.cadastrar(dto));

        // Assert
        assertEquals("O nome do departamento é obrigatório", erro.getMessage());
        verify(departamentoRepository, never()).save(any());
    }

    @Test
    void deveRejeitarCadastroQuandoEmpresaNaoExistir() {
        // Arrange
        DepartamentoDTO dto = novoDepartamento("Tecnologia");
        when(empresaRepository.findById(EMPRESA_ID)).thenReturn(Optional.empty());

        // Act
        RuntimeException erro = assertThrows(
                RuntimeException.class,
                () -> departamentoService.cadastrar(dto));

        // Assert
        assertEquals("Empresa não encontrada", erro.getMessage());
        verify(departamentoRepository, never()).save(any());
    }

    @Test
    void deveRejeitarCadastroQuandoNomeJaExistirNaEmpresa() {
        // Arrange
        Empresa empresa = novaEmpresa();
        DepartamentoDTO dto = novoDepartamento("Tecnologia");
        when(empresaRepository.findById(EMPRESA_ID)).thenReturn(Optional.of(empresa));
        lenient().when(departamentoRepository
                .existsByEmpresaIdAndNomeIgnoreCase(EMPRESA_ID, "Tecnologia"))
                .thenReturn(true);

        // Act
        IllegalArgumentException erro = assertThrows(
                IllegalArgumentException.class,
                () -> departamentoService.cadastrar(dto));

        // Assert
        assertEquals("Já existe um departamento com esse nome na empresa", erro.getMessage());
        verify(departamentoRepository, never()).save(any());
    }

    private DepartamentoDTO novoDepartamento(String nome) {
        return new DepartamentoDTO(
                nome,
                "Ricardo",
                "Diretoria > Tecnologia",
                EMPRESA_ID);
    }

    private Empresa novaEmpresa() {
        return new Empresa(
                EMPRESA_ID,
                "Empresa Exemplo",
                "12345678000199",
                null,
                100000.0);
    }
}
