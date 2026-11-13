package br.com.hestia.indicadores.maturidade;

import br.com.hestia.departamento.model.Departamento;
import br.com.hestia.empresa.model.Empresa;
import br.com.hestia.reutilizavel.service.AcessoRespostaService;
import br.com.hestia.usuario.model.PerfilUsuario;
import br.com.hestia.usuario.model.Usuario;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ColaboracaoDemoControllerTest {

    @TempDir Path pasta;

    @Test
    void calculaFonteDemoERecusaOutraEmpresa() throws Exception {
        var arquivo = pasta.resolve("vinculos.json");
        Files.writeString(arquivo, """
                {"natureza":"EVENTOS_GITHUB_REAIS_COM_VINCULOS_DEMO",
                 "inicio":"2026-11-03","fim":"2026-11-12","populacaoPrsCompleta":true,
                 "pullRequests":[{"repositorio":"tec-ricardo/HestIA","numero":1,
                 "dataMerge":"2026-11-05","empresaId":1,"departamentoId":10,
                 "autorId":42,"quantidadeRevisoes":0}]}
                """);

        var empresa = mock(Empresa.class);
        var departamento = mock(Departamento.class);
        var usuario = mock(Usuario.class);
        var acesso = mock(AcessoRespostaService.class);
        when(empresa.getId()).thenReturn(1L);
        when(empresa.getCnpj()).thenReturn("99999999000272");
        when(departamento.getId()).thenReturn(10L);
        when(departamento.getEmpresa()).thenReturn(empresa);
        when(usuario.getEmpresa()).thenReturn(empresa);
        when(usuario.getDepartamento()).thenReturn(departamento);
        when(usuario.getPerfil()).thenReturn(PerfilUsuario.ADMIN);
        when(acesso.validarEmpresa(usuario, null)).thenReturn(1L);

        var controller = new ColaboracaoDemoController(acesso, new CalculadorasMaturidade(), arquivo.toString());
        var resultado = controller.consultar(usuario);
        assertThat(resultado.natureza()).isEqualTo("EVENTOS_GITHUB_REAIS_COM_VINCULOS_DEMO");
        assertThat(resultado.fragil()).isTrue();
        assertThat(resultado.fator().nota()).isEqualByComparingTo("0");
        assertThat(resultado.fator().coberturaClassificacao()).isEqualByComparingTo("1");

        when(empresa.getCnpj()).thenReturn("12345678000195");
        assertThatThrownBy(() -> controller.consultar(usuario))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode().value()).isEqualTo(403));
    }
}
