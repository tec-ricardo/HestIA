package br.com.hestia.indicadores.maturidade;

import br.com.hestia.usuario.model.*;
import br.com.hestia.empresa.model.Empresa;
import br.com.hestia.departamento.model.Departamento;
import br.com.hestia.departamento.repository.DepartamentoRepository;
import br.com.hestia.reutilizavel.service.AcessoRespostaService;
import br.com.hestia.reutilizavel.repository.RespostaReutilizavelRepository;
import br.com.hestia.indicadores.service.ComposicaoMaturidadeService;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDate;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class ResultadoMaturidadeServiceTest {
    private final ConfiguracaoMaturidade config=new ConfiguracaoMaturidade();
    private final AdaptadorSinteticoMaturidade sintetico=mock(AdaptadorSinteticoMaturidade.class);
    private final AdaptadorOperacionalMaturidade operacional=mock(AdaptadorOperacionalMaturidade.class);
    private final DepartamentoRepository departamentos=mock(DepartamentoRepository.class);
    private final ResultadoMaturidadeService service=new ResultadoMaturidadeService(config,sintetico,operacional,
        new AcessoRespostaService(mock(RespostaReutilizavelRepository.class)),departamentos,new ComposicaoMaturidadeService());
    private Usuario usuario(PerfilUsuario perfil){
        Usuario u=new Usuario();u.setId(1L);u.setEmpresa(new Empresa(7L,"Empresa","12345678901234",null,null));u.setPerfil(perfil);
        u.setDepartamento(new Departamento(10L,"Dept",null,null,u.getEmpresa()));return u;
    }
    private ResultadoMaturidadeService.Consulta consulta(String dep,String hash){return new ResultadoMaturidadeService.Consulta(LocalDate.of(2026,1,1),LocalDate.of(2026,7,31),ResultadoMaturidadeService.Natureza.SINTETICA,hash,null,dep,"Bug");}
    private List<FatorCalculado> vazios(){var c=new CalculadorasMaturidade();return List.of("aiLiteracy","governanca","usoResponsavel","produtividade","finOps","seguranca","colaboracao","desenvolvimentoCompetencias").stream().map(k->c.pendente(k,"SEM_DADOS","fixture","fixture")).toList();}
    @Test void funcionarioNaoConsultaResultadosNovos(){
        assertThatThrownBy(()->service.consultar(usuario(PerfilUsuario.FUNCIONARIO),consulta(null,"A".repeat(64)))).isInstanceOf(ResponseStatusException.class);
        verifyNoInteractions(sintetico,operacional);
    }
    @Test void mapeamentoAusenteNaoAssumeEmpresaUmOuC01(){
        assertThatThrownBy(()->service.consultar(usuario(PerfilUsuario.ADMIN),consulta(null,"A".repeat(64)))).isInstanceOf(ResponseStatusException.class);
        verifyNoInteractions(sintetico);
    }
    @Test void hashInvalidoNaoChegaAFonte(){
        config.getEmpresasOrigem().put(7L,"C09");
        assertThatThrownBy(()->service.consultar(usuario(PerfilUsuario.ADMIN),consulta(null,"todos"))).isInstanceOf(ResponseStatusException.class);
        verifyNoInteractions(sintetico);
    }
    @Test void derivaOrigemDaEmpresaAutenticadaEExigeVersaoDaMeta(){
        config.getEmpresasOrigem().put(7L,"C09");config.getMetasDias().put("7:*:Bug",new java.math.BigDecimal("5"));
        when(sintetico.calcular(any())).thenReturn(vazios());
        var r=service.consultar(usuario(PerfilUsuario.ADMIN),consulta(null,"A".repeat(64)));
        var captura=ArgumentCaptor.forClass(AdaptadorSinteticoMaturidade.Escopo.class);verify(sintetico).calcular(captura.capture());
        assertThat(captura.getValue().empresa()).isEqualTo("C09");assertThat(captura.getValue().metaDias()).isNull();
        assertThat(r.scoreGlobal()).isNull();assertThat(r.scoreParcial()).isNull();assertThat(r.fusoOrigem()).isEqualTo("NAO_DECLARADO");
    }
    @Test void gestorFicaNoDepartamentoMapeado(){
        var gestor=usuario(PerfilUsuario.GESTOR);config.getEmpresasOrigem().put(7L,"C09");config.getDepartamentosOrigem().put(10L,"D09");
        when(departamentos.findById(10L)).thenReturn(Optional.of(gestor.getDepartamento()));when(sintetico.calcular(any())).thenReturn(vazios());
        var r=service.consultar(gestor,consulta(null,"A".repeat(64)));assertThat(r.departamentoOrigem()).isEqualTo("D09");
        assertThatThrownBy(()->service.consultar(gestor,consulta("D99","A".repeat(64)))).isInstanceOf(ResponseStatusException.class);
    }
    @Test void naoMisturaSnapshotSinteticoComFonteOperacional(){
        var q=new ResultadoMaturidadeService.Consulta(LocalDate.now(),LocalDate.now(),ResultadoMaturidadeService.Natureza.OPERACIONAL,"A".repeat(64),null,null,null);
        assertThatThrownBy(()->service.consultar(usuario(PerfilUsuario.ADMIN),q)).isInstanceOf(ResponseStatusException.class);
        verifyNoInteractions(operacional);
    }
}
