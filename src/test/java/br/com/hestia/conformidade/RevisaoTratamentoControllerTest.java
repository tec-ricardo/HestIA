package br.com.hestia.conformidade;

import br.com.hestia.autenticacao.config.ControleAcessoInterceptor;
import br.com.hestia.autenticacao.contexto.EmpresaAtual;
import br.com.hestia.autenticacao.model.SessaoUsuario;
import br.com.hestia.autenticacao.service.*;
import br.com.hestia.conformidade.controller.TratamentoNaoConformidadeController;
import br.com.hestia.conformidade.dto.*;
import br.com.hestia.conformidade.model.*;
import br.com.hestia.conformidade.service.TratamentoNaoConformidadeService;
import br.com.hestia.empresa.model.Empresa;
import br.com.hestia.shared.api.ApiExceptionHandler;
import br.com.hestia.usuario.model.*;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import java.time.LocalDateTime;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class RevisaoTratamentoControllerTest {
    private List<MockHttpServletRequestBuilder> operacoes() {
        return List.of(get("/tratamentos-nao-conformidade/10/historico"),
                post("/tratamentos-nao-conformidade/10/revisoes").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decisao\":\"CONFIRMAR\",\"justificativa\":\"Conferido\",\"versaoRegistro\":0}"),
                get("/tratamentos-nao-conformidade/10"),
                get("/tratamentos-nao-conformidade"),
                put("/tratamentos-nao-conformidade/10").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"ABERTA\",\"versaoRegistro\":0}"),
                post("/tratamentos-nao-conformidade").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"avaliacaoId\":10}"));
    }

    @Test void deveNegarTodasAsOperacoesSemSessao() throws Exception {
        var service = mock(TratamentoNaoConformidadeService.class);
        var auth = mock(AutenticacaoService.class);
        when(auth.validar(null)).thenThrow(new CredenciaisInvalidasException("Sessão obrigatória"));
        var mvc = MockMvcBuilders.standaloneSetup(new TratamentoNaoConformidadeController(service))
                .addInterceptors(new ControleAcessoInterceptor(auth, new EmpresaAtual())).build();
        for (var op : operacoes()) mvc.perform(op).andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
    }

    @Test void deveNegarTodasAsOperacoesParaFuncionarioMesmoSendoResponsavel() throws Exception {
        var service = mock(TratamentoNaoConformidadeService.class);
        var auth = mock(AutenticacaoService.class);
        var sessao = mock(SessaoUsuario.class);
        var usuario = new Usuario();
        usuario.setId(7L); usuario.setPerfil(PerfilUsuario.FUNCIONARIO);
        usuario.setEmpresa(new Empresa(1L, "Teste", "00000000000000", null, null));
        when(auth.validar("token")).thenReturn(sessao); when(sessao.getUsuario()).thenReturn(usuario);
        var mvc = MockMvcBuilders.standaloneSetup(new TratamentoNaoConformidadeController(service))
                .addInterceptors(new ControleAcessoInterceptor(auth, new EmpresaAtual())).build();
        for (var op : operacoes()) mvc.perform(op.header("Authorization", "Bearer token")).andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }

    @Test void deveValidarRevisaoESemRotasDeEdicaoDoHistorico() throws Exception {
        var service = mock(TratamentoNaoConformidadeService.class);
        var mvc = MockMvcBuilders.standaloneSetup(new TratamentoNaoConformidadeController(service))
                .setControllerAdvice(new ApiExceptionHandler()).build();
        for (String body : List.of("{}", "{\"decisao\":\"CONFIRMAR\",\"justificativa\":\" \",\"versaoRegistro\":0}",
                "{\"decisao\":\"REVISAR\",\"justificativa\":\"Conferido\",\"versaoRegistro\":-1}",
                "{\"decisao\":\"OUTRA\",\"justificativa\":\"Conferido\",\"versaoRegistro\":0}",
                "{\"decisao\":\"CONFIRMAR\",\"justificativa\":\"" + "x".repeat(1001) + "\",\"versaoRegistro\":0}")) {
            mvc.perform(post("/tratamentos-nao-conformidade/10/revisoes").contentType(MediaType.APPLICATION_JSON)
                    .content(body)).andExpect(status().isBadRequest());
        }
        mvc.perform(put("/tratamentos-nao-conformidade/10/historico")).andExpect(status().isMethodNotAllowed());
        mvc.perform(delete("/tratamentos-nao-conformidade/10/historico")).andExpect(status().isMethodNotAllowed());
        verifyNoInteractions(service);
    }

    @Test void deveExporDecisaoVigenteSeparadaDaOriginalEIgnorarAutorForjado() throws Exception {
        var service = mock(TratamentoNaoConformidadeService.class);
        var dto = new RevisaoTratamentoDTO(DecisaoRevisao.REVISAR, StatusConformidade.CONFORME, "Autorização comprovada", 0L);
        var resumo = new TratamentoResumoDTO(10L, 20L, 1L, 2L, StatusTratamento.ABERTA,
                StatusConformidade.NAO_CONFORME, 7L, LocalDateTime.now(), null, null, 1L,
                StatusConformidade.CONFORME, 1L);
        when(service.revisar(10L, dto)).thenReturn(new TratamentoDetalheDTO(resumo, null, null, null));
        var mvc = MockMvcBuilders.standaloneSetup(new TratamentoNaoConformidadeController(service)).build();
        mvc.perform(post("/tratamentos-nao-conformidade/10/revisoes").contentType(MediaType.APPLICATION_JSON)
                .content("{\"decisao\":\"REVISAR\",\"resultado\":\"CONFORME\",\"justificativa\":\"Autorização comprovada\",\"versaoRegistro\":0,\"autorId\":999,\"empresaId\":999}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tratamento.resultadoOriginal").value("NAO_CONFORME"))
                .andExpect(jsonPath("$.tratamento.resultadoVigente").value("CONFORME"))
                .andExpect(jsonPath("$.tratamento.versaoRegistro").value(1));
        verify(service).revisar(10L, dto);
    }
}
