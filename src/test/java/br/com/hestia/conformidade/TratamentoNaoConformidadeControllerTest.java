package br.com.hestia.conformidade;

import br.com.hestia.autenticacao.config.ControleAcessoInterceptor;
import br.com.hestia.autenticacao.contexto.EmpresaAtual;
import br.com.hestia.autenticacao.model.SessaoUsuario;
import br.com.hestia.autenticacao.service.AutenticacaoService;
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
import java.time.LocalDateTime;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class TratamentoNaoConformidadeControllerTest {
    @Test
    void deveValidarAvaliacaoObrigatoriaEPositiva() throws Exception {
        var service = mock(TratamentoNaoConformidadeService.class);
        var mvc = MockMvcBuilders.standaloneSetup(new TratamentoNaoConformidadeController(service))
                .setControllerAdvice(new ApiExceptionHandler()).build();
        for (String body : new String[]{"{}", "{\"avaliacaoId\":-1}"}) {
            mvc.perform(post("/tratamentos-nao-conformidade").contentType(MediaType.APPLICATION_JSON)
                    .content(body)).andExpect(status().isBadRequest());
        }
        verifyNoInteractions(service);
    }

    @Test
    void deveValidarAtualizacaoAntesDeChamarServico() throws Exception {
        var service = mock(TratamentoNaoConformidadeService.class);
        var mvc = MockMvcBuilders.standaloneSetup(new TratamentoNaoConformidadeController(service))
                .setControllerAdvice(new ApiExceptionHandler()).build();
        for (String body : new String[]{"{}", "{\"status\":\"ABERTA\",\"versaoRegistro\":-1}",
                "{\"status\":\"ABERTA\",\"versaoRegistro\":0,\"responsavelId\":-1}",
                "{\"status\":\"INVALIDO\",\"versaoRegistro\":0}",
                "{\"status\":\"ABERTA\",\"versaoRegistro\":0,\"acaoCorretiva\":\"" + "x".repeat(1001) + "\"}"}) {
            mvc.perform(put("/tratamentos-nao-conformidade/5")
                    .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isBadRequest());
        }
        mvc.perform(put("/tratamentos-nao-conformidade/5").contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"ABERTA\",\"versaoRegistro\":0}")).andExpect(status().isOk());
        verify(service).atualizar(5L, new AtualizacaoTratamentoDTO(null, null, StatusTratamento.ABERTA, 0L));
        verifyNoMoreInteractions(service);
    }

    @Test
    void deveRetornarLocalizacaoDoTratamentoAberto() throws Exception {
        var service = mock(TratamentoNaoConformidadeService.class);
        var resumo = new TratamentoResumoDTO(5L, 10L, 1L, 2L, StatusTratamento.ABERTA,
                StatusConformidade.NAO_CONFORME, 7L, LocalDateTime.of(2026, 1, 1, 12, 0), null, null, 0L, StatusConformidade.NAO_CONFORME, 0L);
        when(service.abrir(new AberturaTratamentoDTO(10L)))
                .thenReturn(new TratamentoDetalheDTO(resumo, null, null, null));
        var mvc = MockMvcBuilders.standaloneSetup(new TratamentoNaoConformidadeController(service)).build();
        mvc.perform(post("/tratamentos-nao-conformidade").contentType(MediaType.APPLICATION_JSON)
                .content("{\"avaliacaoId\":10}")).andExpect(status().isCreated())
                .andExpect(header().string("Location", "/tratamentos-nao-conformidade/5"))
                .andExpect(jsonPath("$.tratamento.status").value("ABERTA"));
    }

    @Test
    void deveNegarFuncionarioAntesDeExecutarTratamento() throws Exception {
        var service = mock(TratamentoNaoConformidadeService.class);
        var auth = mock(AutenticacaoService.class);
        var sessao = mock(SessaoUsuario.class);
        var u = new Usuario();
        u.setId(7L); u.setPerfil(PerfilUsuario.FUNCIONARIO);
        u.setEmpresa(new Empresa(1L, "Teste", "00000000000000", null, null));
        when(auth.validar("token")).thenReturn(sessao);
        when(sessao.getUsuario()).thenReturn(u);
        var mvc = MockMvcBuilders.standaloneSetup(new TratamentoNaoConformidadeController(service))
                .addInterceptors(new ControleAcessoInterceptor(auth, new EmpresaAtual())).build();
        mvc.perform(get("/tratamentos-nao-conformidade").header("Authorization", "Bearer token"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }
}
