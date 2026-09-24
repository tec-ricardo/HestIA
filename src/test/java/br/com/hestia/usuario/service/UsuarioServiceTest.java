package br.com.hestia.usuario.service;

import br.com.hestia.auditoria.service.LogAuditoriaService;
import br.com.hestia.departamento.repository.DepartamentoRepository;
import br.com.hestia.empresa.repository.EmpresaRepository;
import br.com.hestia.usuario.dto.UsuarioDTO;
import br.com.hestia.usuario.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class UsuarioServiceTest {

    @Test
    void deveImpedirCadastroComEmailDuplicado() {

        UsuarioRepository usuarioRepository =
                mock(UsuarioRepository.class);

        EmpresaRepository empresaRepository =
                mock(EmpresaRepository.class);

        DepartamentoRepository departamentoRepository =
                mock(DepartamentoRepository.class);

        PasswordEncoder passwordEncoder =
                mock(PasswordEncoder.class);

        LogAuditoriaService logAuditoriaService =
                mock(LogAuditoriaService.class);

        UsuarioService service =
                new UsuarioService(
                        usuarioRepository,
                        empresaRepository,
                        departamentoRepository,
                        passwordEncoder,
                        logAuditoriaService
                );

        UsuarioDTO dto =
                new UsuarioDTO();

        dto.setEmail(
                "teste@email.com"
        );

        when(
                usuarioRepository.existsByEmail(
                        "teste@email.com"
                )
        ).thenReturn(true);

        RuntimeException exception =
                assertThrows(
                        RuntimeException.class,
                        () -> service.criar(dto)
                );

        assertEquals(
                "Já existe um usuário cadastrado com esse e-mail.",
                exception.getMessage()
        );

        verify(
                usuarioRepository
        ).existsByEmail(
                "teste@email.com"
        );

        verifyNoInteractions(
                logAuditoriaService
        );
    }
}