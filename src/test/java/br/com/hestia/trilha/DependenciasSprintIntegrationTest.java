package br.com.hestia.trilha;

import br.com.hestia.departamento.model.Departamento;
import br.com.hestia.empresa.model.Empresa;
import br.com.hestia.ferramenta.model.FerramentaIA;
import br.com.hestia.ferramenta.model.TipoFerramentaIA;
import br.com.hestia.indicadores.controller.QualidadeDadosController;
import br.com.hestia.indicadores.service.QualidadeDadosService;
import br.com.hestia.microtreinamento.model.ConclusaoMicrotreinamento;
import br.com.hestia.registro.model.FinalidadeUso;
import br.com.hestia.registro.model.RegistroUsoIA;
import br.com.hestia.registro.model.StatusExecucao;
import br.com.hestia.reutilizavel.service.AcessoRespostaService;
import br.com.hestia.trilha.service.TrilhaService;
import br.com.hestia.usuario.model.PerfilUsuario;
import br.com.hestia.usuario.model.Usuario;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DataJpaTest
@Import({TrilhaService.class, QualidadeDadosService.class, AcessoRespostaService.class})
class DependenciasSprintIntegrationTest {
    @Autowired EntityManager em;
    @Autowired TrilhaService trilhas;
    @Autowired QualidadeDadosService qualidade;

    Empresa a;
    Empresa b;
    Usuario admin;
    Usuario funcionario;
    Usuario outro;

    @BeforeEach
    void preparar() {
        a = new Empresa(null, "A", "11111111111111", null, null);
        b = new Empresa(null, "B", "22222222222222", null, null);
        em.persist(a);
        em.persist(b);

        admin = usuario(a, PerfilUsuario.ADMIN, "admin");
        funcionario = usuario(a, PerfilUsuario.FUNCIONARIO, "func");
        outro = usuario(b, PerfilUsuario.ADMIN, "outro");
    }

    Usuario usuario(Empresa empresa, PerfilUsuario perfil, String nome) {
        var departamento = new Departamento(null, nome, null, null, empresa);
        em.persist(departamento);

        var usuario = new Usuario();
        usuario.setNome(nome);
        usuario.setEmail(nome + "@teste.invalid");
        usuario.setSenha("hash");
        usuario.setCargo("Analista");
        usuario.setPerfil(perfil);
        usuario.setEmpresa(empresa);
        usuario.setDepartamento(departamento);
        em.persist(usuario);
        return usuario;
    }

    @Test
    void integraTrilhaProgressoHistoricoEDatasSemDuplicar() {
        var trilha = trilhas.criar(admin, "Trilha de governança", List.of(1L, 2L));
        var progresso = trilhas.iniciar(funcionario, trilha.id(), funcionario.getId());
        assertThat(progresso.status()).isEqualTo("NAO_INICIADA");

        var data = LocalDateTime.now().minusDays(2);
        var conclusao = new ConclusaoMicrotreinamento(funcionario.getId(), 1L, data, true);
        em.persist(conclusao);
        em.flush();

        progresso = trilhas.consultar(funcionario, progresso.id());
        assertThat(progresso.status()).isEqualTo("EM_ANDAMENTO");
        assertThat(progresso.percentual()).isEqualTo(50);
        assertThat(progresso.etapas().getFirst().conclusaoId()).isEqualTo(conclusao.getId());
        assertThat(trilhas.porConclusao(admin, conclusao.getId())).hasSize(1);

        var ultima = data.plusDays(1);
        em.persist(new ConclusaoMicrotreinamento(funcionario.getId(), 2L, ultima, true));
        em.flush();

        progresso = trilhas.consultar(funcionario, progresso.id());
        assertThat(progresso.status()).isEqualTo("CONCLUIDA");
        assertThat(progresso.percentual()).isEqualTo(100);
        assertThat(progresso.dataConclusao()).isEqualTo(ultima);
        assertThat(progresso.empresaId()).isEqualTo(a.getId());
        assertThat(trilhas.iniciar(funcionario, trilha.id(), funcionario.getId()).id())
                .isEqualTo(progresso.id());
        assertThat(trilhas.historico(admin, funcionario.getId())).hasSize(1);
    }

    @Test
    void protegeEmpresaUsuarioECatalogo() {
        assertThatThrownBy(() -> trilhas.criar(funcionario, "x", List.of(1L)))
                .isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> trilhas.criar(admin, "x", List.of(1L, 1L)))
                .isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> trilhas.criar(admin, "x", List.of(999L)))
                .isInstanceOf(ResponseStatusException.class);

        var trilha = trilhas.criar(admin, "x", List.of(1L));
        assertThatThrownBy(() -> trilhas.iniciar(outro, trilha.id(), funcionario.getId()))
                .isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> trilhas.iniciar(funcionario, admin.getId(), trilha.id()))
                .isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> trilhas.historico(admin, outro.getId()))
                .isInstanceOf(ResponseStatusException.class);
    }

    RegistroUsoIA registro(Usuario usuario, StatusExecucao statusExecucao, int tokens) {
        var ferramenta = new FerramentaIA(
                "IA" + UUID.randomUUID(), "Fornecedor", "Desc", TipoFerramentaIA.CHATBOT,
                "Apoio", null, false, usuario.getEmpresa()
        );
        em.persist(ferramenta);

        var registro = new RegistroUsoIA(
                usuario, ferramenta, "modelo", LocalDateTime.now().minusHours(1),
                FinalidadeUso.GERACAO_CODIGO, tokens, 10, BigDecimal.ONE, statusExecucao, 100L
        );
        em.persist(registro);
        em.flush();
        return registro;
    }

    @Test
    void calculaCoberturaRealESinalizaSemTransformarAusenciaEmZero() {
        registro(funcionario, StatusExecucao.SUCESSO, 20);
        registro(funcionario, StatusExecucao.ERRO, 20);
        registro(outro, StatusExecucao.SUCESSO, 20);

        var hoje = LocalDate.now();
        var resultado = qualidade.consultar(admin, hoje.minusDays(1), hoje, 2);

        assertThat(resultado.totalRegistros()).isEqualTo(2);
        assertThat(resultado.registrosElegiveis()).isEqualTo(1);
        assertThat(resultado.cobertura()).isEqualTo(50);
        assertThat(resultado.calculavel()).isFalse();
        assertThat(resultado.problemas()).extracting(QualidadeDadosService.Problema::codigo)
                .contains("EXECUCAO_SEM_SUCESSO");
        assertThatThrownBy(() -> qualidade.exigirDadosMinimos(resultado))
                .isInstanceOf(ResponseStatusException.class);

        var vazio = qualidade.consultar(admin, hoje.minusDays(10), hoje.minusDays(5), 1);
        assertThat(vazio.cobertura()).isNull();
        assertThat(vazio.status()).isEqualTo("DADOS_INSUFICIENTES");
    }

    @Test
    void excluiTodasAsCopiasDeUmIdentificadorDuplicado() {
        var registro = registro(funcionario, StatusExecucao.SUCESSO, 10);
        var hoje = LocalDate.now();
        var resultado = qualidade.avaliar(
                a.getId(), null, hoje.minusDays(1), hoje, 1, List.of(registro, registro)
        );

        assertThat(resultado.registrosElegiveis()).isZero();
        assertThat(resultado.problemas()).extracting(QualidadeDadosService.Problema::codigo)
                .containsOnly("DUPLICADO");
    }

    @Test
    void sinalizaDepartamentoDeOutraEmpresaEPreservaRegistro() {
        funcionario.setDepartamento(outro.getDepartamento());
        var registro = registro(funcionario, StatusExecucao.SUCESSO, 10);
        var registroId = registro.getId();
        var adminId = admin.getId();
        em.clear();

        var hoje = LocalDate.now();
        var resultado = qualidade.consultar(
                em.find(Usuario.class, adminId), hoje.minusDays(1), hoje, 1
        );

        assertThat(resultado.totalRegistros()).isEqualTo(1);
        assertThat(resultado.registrosElegiveis()).isZero();
        assertThat(resultado.registrosExcluidos()).isEqualTo(1);
        assertThat(resultado.problemas()).containsExactly(
                new QualidadeDadosService.Problema(registroId, "DEPARTAMENTO_FORA_ESCOPO")
        );
        assertThat(em.find(RegistroUsoIA.class, registroId)).isNotNull();
    }

    @Test
    void gestorSoConsultaQualidadeDoProprioDepartamento() {
        var gestor = usuario(a, PerfilUsuario.GESTOR, "gestor");
        gestor.setDepartamento(funcionario.getDepartamento());
        registro(funcionario, StatusExecucao.SUCESSO, 10);
        registro(admin, StatusExecucao.SUCESSO, 10);
        em.flush();

        var hoje = LocalDate.now();
        var resultado = qualidade.consultar(gestor, hoje.minusDays(1), hoje, 1);

        assertThat(resultado.totalRegistros()).isEqualTo(1);
        assertThat(resultado.departamentoId()).isEqualTo(funcionario.getDepartamento().getId());
    }

    @Test
    void endpointDeValidacaoBloqueiaResultadosSemAmostraMinima() throws Exception {
        var controller = new QualidadeDadosController(qualidade);
        var mvc = MockMvcBuilders.standaloneSetup(controller).build();
        var hoje = LocalDate.now();

        mvc.perform(get("/indicadores/dados/validacao")
                        .requestAttr("usuarioAutenticado", admin)
                        .param("inicio", hoje.minusDays(1).toString())
                        .param("fim", hoje.toString())
                        .param("minimoRegistros", "2"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void funcionarioConsultaSomentePropriosRegistros() {
        registro(admin, StatusExecucao.SUCESSO, 10);
        registro(funcionario, StatusExecucao.SUCESSO, 10);

        var hoje = LocalDate.now();
        var resultado = qualidade.consultar(funcionario, hoje.minusDays(1), hoje, 1);

        assertThat(resultado.totalRegistros()).isEqualTo(1);
        assertThat(resultado.calculavel()).isTrue();
        assertThatCode(() -> qualidade.exigirDadosMinimos(resultado)).doesNotThrowAnyException();
        assertThatThrownBy(() -> qualidade.consultar(admin, hoje, hoje.minusDays(1), 1))
                .isInstanceOf(ResponseStatusException.class);
    }
}