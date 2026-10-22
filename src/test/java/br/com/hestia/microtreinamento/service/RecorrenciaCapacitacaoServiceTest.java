package br.com.hestia.microtreinamento.service;

import br.com.hestia.empresa.model.Empresa;
import br.com.hestia.microtreinamento.dto.OcorrenciaErroDTO;
import br.com.hestia.microtreinamento.model.ConclusaoMicrotreinamento;
import br.com.hestia.microtreinamento.model.TipoErro;
import br.com.hestia.microtreinamento.repository.ConclusaoMicrotreinamentoRepository;
import br.com.hestia.reutilizavel.service.AcessoRespostaService;
import br.com.hestia.usuario.model.PerfilUsuario;
import br.com.hestia.usuario.model.Usuario;
import br.com.hestia.usuario.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RecorrenciaCapacitacaoServiceTest {

    private final ConclusaoMicrotreinamentoRepository conclusoes =
            mock(ConclusaoMicrotreinamentoRepository.class);

    private final UsuarioRepository usuarios =
            mock(UsuarioRepository.class);

    private final AcessoRespostaService acesso =
            mock(AcessoRespostaService.class);

    private final RecorrenciaCapacitacaoService service =
            new RecorrenciaCapacitacaoService(
                    conclusoes,
                    usuarios,
                    acesso,
                    new RecorrenciaErroService()
            );

    private final LocalDateTime dataConclusao =
            LocalDateTime.of(2025, 1, 15, 12, 0);

    private Usuario autenticado;
    private ConclusaoMicrotreinamento conclusao;

    @BeforeEach
    void preparar() {
        autenticado = usuario(10L, 1L, PerfilUsuario.FUNCIONARIO);

        conclusao = new ConclusaoMicrotreinamento(
                10L,
                1L,
                dataConclusao,
                true
        );

        when(acesso.validarEmpresa(autenticado, null))
                .thenReturn(1L);

        when(conclusoes.findById(100L))
                .thenReturn(Optional.of(conclusao));

        when(usuarios.findById(10L))
                .thenReturn(Optional.of(autenticado));
    }

    @Test
    void deveUsarUsuarioTemaEDataDaConclusaoPersistida() {
        var resultado = consultar(List.of(
                new OcorrenciaErroDTO(
                        "antes",
                        1L,
                        10L,
                        TipoErro.DADOS_SENSIVEIS,
                        dataConclusao.minusDays(1)
                ),
                new OcorrenciaErroDTO(
                        "depois",
                        1L,
                        10L,
                        TipoErro.DADOS_SENSIVEIS,
                        dataConclusao.plusDays(1)
                )
        ));

        assertEquals(100L, resultado.conclusaoId());
        assertEquals(1L, resultado.microtreinamentoId());
        assertEquals(
                dataConclusao,
                resultado.recorrencia().dataConclusao()
        );
        assertEquals(1, resultado.recorrencia().ocorrenciasAntes());
        assertEquals(1, resultado.recorrencia().ocorrenciasDepois());
    }

    @Test
    void deveBloquearConclusaoDeOutraEmpresa() {
        when(usuarios.findById(10L)).thenReturn(Optional.of(
                usuario(10L, 2L, PerfilUsuario.FUNCIONARIO)
        ));

        assertEquals(404, falha().getStatusCode().value());
    }

    @Test
    void deveBloquearFuncionarioConsultandoOutroUsuario() {
        conclusao.setUsuarioId(20L);

        when(usuarios.findById(20L)).thenReturn(Optional.of(
                usuario(20L, 1L, PerfilUsuario.FUNCIONARIO)
        ));

        assertEquals(404, falha().getStatusCode().value());
    }

    @Test
    void devePermitirGestorDaMesmaEmpresa() {
        autenticado.setPerfil(PerfilUsuario.GESTOR);
        conclusao.setUsuarioId(20L);

        when(usuarios.findById(20L)).thenReturn(Optional.of(
                usuario(20L, 1L, PerfilUsuario.FUNCIONARIO)
        ));

        assertEquals(
                20L,
                consultar(List.of()).recorrencia().usuarioId()
        );
    }

    @Test
    void deveRejeitarCapacitacaoNaoConcluida() {
        conclusao.setConcluido(false);

        assertEquals(409, falha().getStatusCode().value());
    }

    @Test
    void deveRejeitarErroDeTemaDiferente() {
        var exception = assertThrows(
                ResponseStatusException.class,
                () -> service.consultar(
                        autenticado,
                        100L,
                        TipoErro.VIOLACAO_LGPD,
                        dataConclusao.plusDays(30),
                        List.of()
                )
        );

        assertEquals(400, exception.getStatusCode().value());
    }

    @Test
    void deveRejeitarDataDeObservacaoFutura() {
        var exception = assertThrows(
                ResponseStatusException.class,
                () -> service.consultar(
                        autenticado,
                        100L,
                        TipoErro.DADOS_SENSIVEIS,
                        LocalDateTime.now().plusDays(1),
                        List.of()
                )
        );

        assertEquals(400, exception.getStatusCode().value());
    }

    private RecorrenciaCapacitacaoService.Resultado consultar(
            List<OcorrenciaErroDTO> ocorrencias
    ) {
        return service.consultar(
                autenticado,
                100L,
                TipoErro.DADOS_SENSIVEIS,
                dataConclusao.plusDays(30),
                ocorrencias
        );
    }

    private ResponseStatusException falha() {
        return assertThrows(
                ResponseStatusException.class,
                () -> consultar(List.of())
        );
    }

    private Usuario usuario(
            Long id,
            Long empresaId,
            PerfilUsuario perfil
    ) {
        Empresa empresa = new Empresa();
        empresa.setId(empresaId);

        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setEmpresa(empresa);
        usuario.setPerfil(perfil);
        usuario.setAtivo(true);

        return usuario;
    }
}