package br.com.hestia.trilha;
import br.com.hestia.trilha.service.TrilhaService;
import br.com.hestia.indicadores.service.QualidadeDadosService;
import br.com.hestia.reutilizavel.service.AcessoRespostaService;
import br.com.hestia.usuario.model.*;
import br.com.hestia.empresa.model.Empresa;
import br.com.hestia.departamento.model.Departamento;
import br.com.hestia.microtreinamento.model.ConclusaoMicrotreinamento;
import br.com.hestia.ferramenta.model.*;
import br.com.hestia.registro.model.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;
import java.time.*;
import java.math.BigDecimal;
import static org.assertj.core.api.Assertions.*;

@DataJpaTest @Import({TrilhaService.class,QualidadeDadosService.class,AcessoRespostaService.class})
class DependenciasSprintIntegrationTest {
    @Autowired EntityManager em; @Autowired TrilhaService trilhas; @Autowired QualidadeDadosService qualidade;
    Empresa a,b; Usuario admin,funcionario,outro;
    @BeforeEach void preparar(){
        a=new Empresa(null,"A","11111111111111",null,null); b=new Empresa(null,"B","22222222222222",null,null);
        em.persist(a);em.persist(b);admin=usuario(a,PerfilUsuario.ADMIN,"admin");funcionario=usuario(a,PerfilUsuario.FUNCIONARIO,"func");outro=usuario(b,PerfilUsuario.ADMIN,"outro");
    }
    Usuario usuario(Empresa empresa,PerfilUsuario perfil,String nome){
        var d=new Departamento(null,nome,null,null,empresa);em.persist(d);
        var u=new Usuario();u.setNome(nome);u.setEmail(nome+"@teste.invalid");u.setSenha("hash");u.setCargo("Analista");u.setPerfil(perfil);u.setEmpresa(empresa);u.setDepartamento(d);em.persist(u);return u;
    }
    @Test void integraTrilhaProgressoHistoricoEDatasSemDuplicar(){
        var t=trilhas.criar(admin,"Trilha de governança",List.of(1L,2L));
        var p=trilhas.iniciar(funcionario,t.id(),funcionario.getId());
        assertThat(p.status()).isEqualTo("NAO_INICIADA");
        var data=LocalDateTime.now().minusDays(2);
        var c=new ConclusaoMicrotreinamento(funcionario.getId(),1L,data,true);em.persist(c);em.flush();
        p=trilhas.consultar(funcionario,p.id());
        assertThat(p.status()).isEqualTo("EM_ANDAMENTO");assertThat(p.percentual()).isEqualTo(50);
        assertThat(p.etapas().getFirst().conclusaoId()).isEqualTo(c.getId());
        assertThat(trilhas.porConclusao(admin,c.getId())).hasSize(1);
        var ultima=data.plusDays(1);em.persist(new ConclusaoMicrotreinamento(funcionario.getId(),2L,ultima,true));em.flush();
        p=trilhas.consultar(funcionario,p.id());
        assertThat(p.status()).isEqualTo("CONCLUIDA");assertThat(p.dataConclusao()).isEqualTo(ultima);
        assertThat(p.empresaId()).isEqualTo(a.getId());
        assertThat(trilhas.iniciar(funcionario,t.id(),funcionario.getId()).id()).isEqualTo(p.id());
        assertThat(trilhas.historico(admin,funcionario.getId())).hasSize(1);
    }
    @Test void protegeEmpresaUsuarioECatalogo(){
        assertThatThrownBy(()->trilhas.criar(funcionario,"x",List.of(1L))).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(()->trilhas.criar(admin,"x",List.of(1L,1L))).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(()->trilhas.criar(admin,"x",List.of(999L))).isInstanceOf(ResponseStatusException.class);
        var t=trilhas.criar(admin,"x",List.of(1L));
        assertThatThrownBy(()->trilhas.iniciar(outro,t.id(),outro.getId())).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(()->trilhas.historico(funcionario,admin.getId())).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(()->trilhas.historico(admin,outro.getId())).isInstanceOf(ResponseStatusException.class);
    }
    RegistroUsoIA registro(Usuario u,StatusExecucao status,int tokens){
        var ferramenta=new FerramentaIA("IA"+UUID.randomUUID(),"Fornecedor","Desc",TipoFerramentaIA.CHATBOT,"Apoio",null,false,u.getEmpresa());em.persist(ferramenta);
        var r=new RegistroUsoIA(u,ferramenta,"modelo",LocalDateTime.now().minusHours(1),FinalidadeUso.GERACAO_CODIGO,tokens,10,BigDecimal.ONE,status,100L);em.persist(r);em.flush();return r;
    }
    @Test void calculaCoberturaRealESinalizaSemTransformarAusenciaEmZero(){
        registro(funcionario,StatusExecucao.SUCESSO,20);registro(funcionario,StatusExecucao.ERRO,20);registro(outro,StatusExecucao.SUCESSO,20);
        var r=qualidade.consultar(admin,LocalDate.now().minusDays(1),LocalDate.now(),2);
        assertThat(r.totalRegistros()).isEqualTo(2);assertThat(r.registrosElegiveis()).isEqualTo(1);
        assertThat(r.cobertura()).isEqualTo(50);assertThat(r.calculavel()).isFalse();
        assertThat(r.problemas()).extracting(QualidadeDadosService.Problema::codigo).contains("EXECUCAO_SEM_SUCESSO");
        assertThatThrownBy(()->qualidade.exigirDadosMinimos(r)).isInstanceOf(ResponseStatusException.class);
        var vazio=qualidade.consultar(admin,LocalDate.now().minusDays(10),LocalDate.now().minusDays(5),1);
        assertThat(vazio.cobertura()).isNull();assertThat(vazio.status()).isEqualTo("DADOS_INSUFICIENTES");
    }
    @Test void excluiTodasAsCopiasDeUmIdentificadorDuplicado(){
        var r=registro(funcionario,StatusExecucao.SUCESSO,10);
        var resultado=qualidade.avaliar(a.getId(),null,LocalDate.now().minusDays(1),LocalDate.now(),1,List.of(r,r));
        assertThat(resultado.registrosElegiveis()).isZero();
        assertThat(resultado.problemas()).extracting(QualidadeDadosService.Problema::codigo).containsOnly("DUPLICADO");
    }
    @Test void gestorSoConsultaQualidadeDoProprioDepartamento(){
        var gestor=usuario(a,PerfilUsuario.GESTOR,"gestor");gestor.setDepartamento(funcionario.getDepartamento());
        registro(funcionario,StatusExecucao.SUCESSO,10);registro(admin,StatusExecucao.SUCESSO,10);em.flush();
        var r=qualidade.consultar(gestor,LocalDate.now().minusDays(1),LocalDate.now(),1);
        assertThat(r.totalRegistros()).isEqualTo(1);assertThat(r.departamentoId()).isEqualTo(funcionario.getDepartamento().getId());
    }
    @Test void endpointDeValidacaoBloqueiaResultadosSemAmostraMinima() throws Exception {
        var controller=new br.com.hestia.indicadores.controller.QualidadeDadosController(qualidade);
        var mvc=org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup(controller).build();
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/indicadores/dados/validacao")
            .requestAttr("usuarioAutenticado",admin).param("inicio",LocalDate.now().minusDays(1).toString())
            .param("fim",LocalDate.now().toString()).param("minimoRegistros","2"))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isUnprocessableEntity());
    }
    @Test void funcionarioConsultaSomentePropriosRegistros(){
        registro(admin,StatusExecucao.SUCESSO,10);registro(funcionario,StatusExecucao.SUCESSO,10);
        var r=qualidade.consultar(funcionario,LocalDate.now().minusDays(1),LocalDate.now(),1);
        assertThat(r.totalRegistros()).isEqualTo(1);assertThat(r.calculavel()).isTrue();
        assertThatCode(()->qualidade.exigirDadosMinimos(r)).doesNotThrowAnyException();
        assertThatThrownBy(()->qualidade.consultar(admin,LocalDate.now(),LocalDate.now().minusDays(1),1)).isInstanceOf(ResponseStatusException.class);
    }
}
