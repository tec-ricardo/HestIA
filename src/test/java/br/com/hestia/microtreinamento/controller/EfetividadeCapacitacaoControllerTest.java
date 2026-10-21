package br.com.hestia.microtreinamento.controller;

import br.com.hestia.autenticacao.config.ControleAcessoInterceptor;
import br.com.hestia.autenticacao.contexto.EmpresaAtual;
import br.com.hestia.autenticacao.model.SessaoUsuario;
import br.com.hestia.autenticacao.service.AutenticacaoService;
import br.com.hestia.autenticacao.service.CredenciaisInvalidasException;
import br.com.hestia.empresa.model.Empresa;
import br.com.hestia.microtreinamento.dto.OcorrenciaErroDTO;
import br.com.hestia.microtreinamento.model.ConclusaoMicrotreinamento;
import br.com.hestia.microtreinamento.model.TipoErro;
import br.com.hestia.microtreinamento.repository.ConclusaoMicrotreinamentoRepository;
import br.com.hestia.microtreinamento.service.*;
import br.com.hestia.reutilizavel.repository.RespostaReutilizavelRepository;
import br.com.hestia.reutilizavel.service.AcessoRespostaService;
import br.com.hestia.usuario.model.PerfilUsuario;
import br.com.hestia.usuario.model.Usuario;
import br.com.hestia.usuario.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class EfetividadeCapacitacaoControllerTest {

    private static final String ROTA =
            "/microtreinamentos/conclusoes/100/efetividade";

    private final ConclusaoMicrotreinamentoRepository conclusoes =
            mock(ConclusaoMicrotreinamentoRepository.class);

    private final UsuarioRepository usuarios =
            mock(UsuarioRepository.class);

    private final FonteRecorrenciaConformidade fonte =
            mock(FonteRecorrenciaConformidade.class);

    private final LocalDateTime dataConclusao =
            LocalDateTime.of(2025, 1, 15, 12, 0);

    private MockMvc mvc;
    private Usuario usuario;
    private ConclusaoMicrotreinamento conclusao;

    @BeforeEach
    void preparar() {
        Empresa empresa = new Empresa();
        empresa.setId(1L);

        usuario = new Usuario();
        usuario.setId(10L);
        usuario.setEmpresa(empresa);
        usuario.setPerfil(PerfilUsuario.FUNCIONARIO);
        usuario.setAtivo(true);

        conclusao = new ConclusaoMicrotreinamento(
                10L,
                2L,
                dataConclusao,
                true
        );

        when(conclusoes.findById(100L))
                .thenReturn(Optional.of(conclusao));

        when(usuarios.findById(10L))
                .thenReturn(Optional.of(usuario));

        when(fonte.consultar(
                eq(1L),
                eq(10L),
                any(TipoErro.class),
                any(LocalDateTime.class),
                any(LocalDateTime.class)
        )).thenReturn(leituraCompleta());

        var calculador = new RecorrenciaErroService();

        var acompanhamento = new RecorrenciaCapacitacaoService(
                conclusoes,
                usuarios,
                new AcessoRespostaService(
                        mock(RespostaReutilizavelRepository.class)
                ),
                calculador
        );

        var consulta = new ConsultaEfetividadeCapacitacaoService(
                acompanhamento,
                fonte,
                calculador,
                new EfetividadeCapacitacaoService()
        );

        var autenticacao = mock(AutenticacaoService.class);
        var sessao = mock(SessaoUsuario.class);

        when(sessao.getUsuario()).thenReturn(usuario);

        when(autenticacao.validar(any())).thenAnswer(invocacao -> {
            if ("token".equals(invocacao.getArgument(0))) {
                return sessao;
            }

            throw new CredenciaisInvalidasException(
                    "Sessão inválida."
            );
        });

        mvc = MockMvcBuilders.standaloneSetup(
                        new EfetividadeCapacitacaoController(consulta)
                )
                .addInterceptors(
                        new ControleAcessoInterceptor(
                                autenticacao,
                                new EmpresaAtual()
                        )
                )
                .build();
    }

    @Test
    void deveConsultarIndicadorComIdentidadeDaSessao()
            throws Exception {
        mvc.perform(get(ROTA)
                        .header("Authorization", "Bearer token")
                        .param(
                                "tipoErro",
                                "FERRAMENTA_NAO_AUTORIZADA"
                        ))
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.indicador.situacao")
                                .value("MELHORIA")
                )
                .andExpect(
                        jsonPath("$.indicador.reducaoPercentual")
                                .value(66.67)
                )
                .andExpect(
                        jsonPath(
                                "$.indicador.acompanhamento"
                                        + ".recorrencia.usuarioId"
                        ).value(10)
                )
                .andExpect(
                        jsonPath("$.cobertura.confirmada").value(true)
                );

        verify(fonte).consultar(
                eq(1L),
                eq(10L),
                eq(TipoErro.FERRAMENTA_NAO_AUTORIZADA),
                eq(dataConclusao),
                any(LocalDateTime.class)
        );
    }

    @Test
    void deveExigirAutenticacao() throws Exception {
        mvc.perform(get(ROTA)
                        .param(
                                "tipoErro",
                                "FERRAMENTA_NAO_AUTORIZADA"
                        ))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(fonte);
    }

    @Test
    void deveBloquearFuncionarioConsultandoOutroUsuario()
            throws Exception {
        conclusao.setUsuarioId(20L);

        Usuario outro = new Usuario();
        outro.setId(20L);
        outro.setEmpresa(usuario.getEmpresa());

        when(usuarios.findById(20L))
                .thenReturn(Optional.of(outro));

        requisitar()
                .andExpect(status().isNotFound());

        verifyNoInteractions(fonte);
    }

    @Test
    void deveBloquearConclusaoDeOutraEmpresa() throws Exception {
        Empresa outraEmpresa = new Empresa();
        outraEmpresa.setId(2L);

        Usuario outro = new Usuario();
        outro.setId(10L);
        outro.setEmpresa(outraEmpresa);

        when(usuarios.findById(10L))
                .thenReturn(Optional.of(outro));

        requisitar()
                .andExpect(status().isNotFound());

        verifyNoInteractions(fonte);
    }

    @Test
    void deveSinalizarDadosInsuficientesSemRegistros()
            throws Exception {
        when(fonte.consultar(
                anyLong(),
                anyLong(),
                any(TipoErro.class),
                any(LocalDateTime.class),
                any(LocalDateTime.class)
        )).thenReturn(new FonteRecorrenciaConformidade.Leitura(
                List.of(),
                new FonteRecorrenciaConformidade.Janela(0, 0),
                new FonteRecorrenciaConformidade.Janela(0, 0),
                true
        ));

        requisitar()
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.indicador.situacao")
                                .value("DADOS_INSUFICIENTES")
                )
                .andExpect(
                        jsonPath("$.indicador.motivo")
                                .value("COBERTURA_NAO_CONFIRMADA")
                )
                .andExpect(
                        jsonPath("$.indicador.reducaoPercentual")
                                .doesNotExist()
                )
                .andExpect(
                        jsonPath("$.cobertura.confirmada")
                                .value(false)
                );
    }

    @Test
    void deveSinalizarPeriodoPosteriorIncompleto()
            throws Exception {
        conclusao.setDataConclusao(
                LocalDateTime.now().minusDays(5)
        );

        requisitar()
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.indicador.situacao")
                                .value("DADOS_INSUFICIENTES")
                )
                .andExpect(
                        jsonPath("$.indicador.motivo")
                                .value("PERIODO_POSTERIOR_INCOMPLETO")
                );
    }

    @Test
    void deveSinalizarTipoSemFonteConfirmavel()
            throws Exception {
        conclusao.setMicrotreinamentoId(1L);

        when(fonte.consultar(
                anyLong(),
                anyLong(),
                eq(TipoErro.DADOS_SENSIVEIS),
                any(LocalDateTime.class),
                any(LocalDateTime.class)
        )).thenReturn(new FonteRecorrenciaConformidade.Leitura(
                List.of(),
                new FonteRecorrenciaConformidade.Janela(0, 0),
                new FonteRecorrenciaConformidade.Janela(0, 0),
                false
        ));

        mvc.perform(get(ROTA)
                        .header("Authorization", "Bearer token")
                        .param("tipoErro", "DADOS_SENSIVEIS"))
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.indicador.situacao")
                                .value("DADOS_INSUFICIENTES")
                )
                .andExpect(
                        jsonPath("$.cobertura.tipoSuportado")
                                .value(false)
                );
    }

    @Test
    void deveRejeitarTipoInvalido() throws Exception {
        mvc.perform(get(ROTA)
                        .header("Authorization", "Bearer token")
                        .param("tipoErro", "INVALIDO"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(fonte);
    }

    private org.springframework.test.web.servlet.ResultActions
    requisitar() throws Exception {
        return mvc.perform(get(ROTA)
                .header("Authorization", "Bearer token")
                .param(
                        "tipoErro",
                        "FERRAMENTA_NAO_AUTORIZADA"
                ));
    }

    private FonteRecorrenciaConformidade.Leitura leituraCompleta() {
        return new FonteRecorrenciaConformidade.Leitura(
                List.of(
                        ocorrencia("a", dataConclusao.minusDays(3)),
                        ocorrencia("b", dataConclusao.minusDays(2)),
                        ocorrencia("c", dataConclusao.minusDays(1)),
                        ocorrencia("d", dataConclusao.plusDays(1))
                ),
                new FonteRecorrenciaConformidade.Janela(3, 3),
                new FonteRecorrenciaConformidade.Janela(2, 2),
                true
        );
    }

    private OcorrenciaErroDTO ocorrencia(
            String id,
            LocalDateTime data
    ) {
        return new OcorrenciaErroDTO(
                id,
                1L,
                10L,
                TipoErro.FERRAMENTA_NAO_AUTORIZADA,
                data
        );
    }
}