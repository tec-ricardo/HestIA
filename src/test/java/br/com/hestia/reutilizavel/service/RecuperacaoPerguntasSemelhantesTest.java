package br.com.hestia.reutilizavel.service;

import br.com.hestia.empresa.model.Empresa;
import br.com.hestia.usuario.model.Usuario;
import br.com.hestia.usuario.model.PerfilUsuario;
import br.com.hestia.reutilizavel.model.RespostaReutilizavel;
import br.com.hestia.reutilizavel.repository.RespostaReutilizavelRepository;
import br.com.hestia.registro.repository.RegistroUsoIARepository;
import br.com.hestia.gamificacao.service.GamificacaoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.server.ResponseStatusException;
import java.util.Set;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.mock;

@DataJpaTest
class RecuperacaoPerguntasSemelhantesTest {
    @Autowired RespostaReutilizavelRepository repository;

    private Usuario usuario(Long empresa, PerfilUsuario perfil) {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setEmpresa(new Empresa(empresa, "Empresa", "12345678901234", null, null));
        usuario.setPerfil(perfil);
        return usuario;
    }

    private RespostaReutilizavel resposta(Long empresa, Set<PerfilUsuario> perfis) {
        var r = new RespostaReutilizavel();
        r.setEmpresaId(empresa);
        r.setPromptOriginal("Como usar IA?");
        r.setRespostaGerada("Conteúdo restrito " + empresa + perfis);
        r.setEmbedding("[1,0]");
        r.setPerfisPermitidos(perfis);
        return repository.saveAndFlush(r);
    }

    private RespostaReutilizavelService service() {
        return new RespostaReutilizavelService(repository, mock(GamificacaoService.class),
                mock(RegistroUsoIARepository.class), new AcessoRespostaService(repository));
    }

    @Test void integraContextoDaEmpresaComFiltroDePerfil() {
        var contexto = new br.com.hestia.autenticacao.contexto.EmpresaAtual();
        contexto.definir(new br.com.hestia.autenticacao.contexto.ContextoEmpresa(1L, 1L));
        var integrado = new RespostaReutilizavelService(repository, mock(GamificacaoService.class),
                mock(RegistroUsoIARepository.class), contexto, new AcessoRespostaService(repository));
        var permitida = resposta(1L, Set.of(PerfilUsuario.FUNCIONARIO));
        resposta(1L, Set.of(PerfilUsuario.GESTOR));
        resposta(2L, Set.of(PerfilUsuario.FUNCIONARIO));
        assertThat(integrado.buscarRespostasAnteriores(usuario(1L, PerfilUsuario.FUNCIONARIO), 1L, "usar"))
                .extracting(RespostaReutilizavel::getId).containsExactly(permitida.getId());
        assertThatThrownBy(() -> integrado.buscarRespostasAnteriores(
                usuario(2L, PerfilUsuario.FUNCIONARIO), 2L, "usar"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test void filtraEmpresaPerfilEElegibilidadeAntesDaRecuperacao() {
        var permitida = resposta(1L, Set.of(PerfilUsuario.FUNCIONARIO));
        resposta(2L, Set.of(PerfilUsuario.FUNCIONARIO));
        resposta(1L, Set.of(PerfilUsuario.GESTOR));
        resposta(1L, Set.of());
        var sensivel = resposta(1L, Set.of(PerfilUsuario.FUNCIONARIO));
        sensivel.setPossuiDadosSensiveis(true);
        var bloqueada = resposta(1L, Set.of(PerfilUsuario.FUNCIONARIO));
        bloqueada.setReutilizavel(false);
        var semEmbedding = resposta(1L, Set.of(PerfilUsuario.FUNCIONARIO));
        semEmbedding.setEmbedding(" ");
        repository.flush();
        var funcionario = usuario(1L, PerfilUsuario.FUNCIONARIO);
        assertThat(service().recuperarPerguntasSemelhantes(funcionario, 1L))
                .extracting(RespostaReutilizavel::getId).containsExactly(permitida.getId());
    }

    @Test void gestorNaoRecebeConteudoExclusivoDeFuncionario() {
        resposta(1L, Set.of(PerfilUsuario.FUNCIONARIO));
        var gestor = resposta(1L, Set.of(PerfilUsuario.GESTOR));
        assertThat(service().buscarRespostasAnteriores(usuario(1L, PerfilUsuario.GESTOR), 1L, "usar"))
                .extracting(RespostaReutilizavel::getId).containsExactly(gestor.getId());
    }

    @Test void limitaDezResultadosDepoisDeFiltrarPermissoes() {
        for (int i = 0; i < 12; i++) resposta(1L, Set.of(PerfilUsuario.FUNCIONARIO));
        for (int i = 0; i < 12; i++) resposta(1L, Set.of(PerfilUsuario.GESTOR));
        assertThat(service().buscarRespostasAnteriores(usuario(1L, PerfilUsuario.FUNCIONARIO), 1L, "usar"))
                .hasSize(10).allMatch(r -> r.getPerfisPermitidos().contains(PerfilUsuario.FUNCIONARIO));
    }

    @Test void rejeitaEmpresaForjadaESessaoIncompleta() {
        assertThatThrownBy(() -> service().recuperarPerguntasSemelhantes(
                usuario(1L, PerfilUsuario.ADMIN), 2L)).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> service().recuperarPerguntasSemelhantes(null, 1L))
                .isInstanceOf(ResponseStatusException.class);
        var invalido = usuario(1L, null);
        assertThatThrownBy(() -> service().recuperarPerguntasSemelhantes(invalido, 1L))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test void naoRetornaConteudoRestritoPorIdNemAmpliaBuscaVazia() {
        var restrita = resposta(2L, Set.of(PerfilUsuario.FUNCIONARIO));
        var acesso = new AcessoRespostaService(repository);
        var funcionario = usuario(1L, PerfilUsuario.FUNCIONARIO);
        assertThatThrownBy(() -> acesso.buscarPorId(funcionario, restrita.getId()))
                .isInstanceOf(ResponseStatusException.class);
        assertThat(service().buscarRespostasAnteriores(funcionario, 1L, "usar")).isEmpty();
    }

    @Test void apenasAdminDaEmpresaPodeAutorizarOuRevogarPerfis() {
        var r = resposta(1L, Set.of());
        var acesso = new AcessoRespostaService(repository);
        assertThatThrownBy(() -> acesso.autorizarPerfis(usuario(1L, PerfilUsuario.GESTOR),
                r.getId(), Set.of(PerfilUsuario.FUNCIONARIO))).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> acesso.autorizarPerfis(usuario(2L, PerfilUsuario.ADMIN),
                r.getId(), Set.of(PerfilUsuario.FUNCIONARIO))).isInstanceOf(ResponseStatusException.class);
        acesso.autorizarPerfis(usuario(1L, PerfilUsuario.ADMIN), r.getId(), Set.of(PerfilUsuario.FUNCIONARIO));
        assertThat(acesso.buscarPorId(usuario(1L, PerfilUsuario.FUNCIONARIO), r.getId()).getId()).isEqualTo(r.getId());
        acesso.autorizarPerfis(usuario(1L, PerfilUsuario.ADMIN), r.getId(), Set.of());
        assertThat(acesso.recuperar(usuario(1L, PerfilUsuario.FUNCIONARIO), 1L, null, null,
                false, Pageable.unpaged())).isEmpty();
    }

    @Test void cadastroUsaAutorEPerfilDaSessao() {
        var dto = new br.com.hestia.reutilizavel.dto.RespostaReutilizavelDTO();
        dto.setUsuarioId(999L);
        dto.setEmpresaId(1L);
        dto.setPromptOriginal("Pergunta");
        dto.setRespostaGerada("Resposta");
        var r = service().cadastrar(dto, usuario(1L, PerfilUsuario.FUNCIONARIO));
        repository.flush();
        assertThat(r.getUsuarioId()).isEqualTo(1L);
        assertThat(r.getPerfisPermitidos()).containsExactly(PerfilUsuario.FUNCIONARIO);
        dto.setEmpresaId(2L);
        assertThatThrownBy(() -> service().cadastrar(dto, usuario(1L, PerfilUsuario.FUNCIONARIO)))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test void apiNaoPermiteContornarFiltrosPorListagemIdOuReutilizacao() throws Exception {
        var permitida = resposta(1L, Set.of(PerfilUsuario.FUNCIONARIO));
        var restrita = resposta(2L, Set.of(PerfilUsuario.FUNCIONARIO));
        var controller = new br.com.hestia.reutilizavel.controller.RespostaReutilizavelController(
                service(), new AcessoRespostaService(repository));
        var mvc = org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup(controller).build();
        var funcionario = usuario(1L, PerfilUsuario.FUNCIONARIO);
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .get("/api/respostas-reutilizaveis")
                .requestAttr("usuarioAutenticado", funcionario))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.length()").value(1))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$[0].id").value(permitida.getId().intValue()));
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .get("/api/respostas-reutilizaveis/empresa/2/buscar").param("prompt", "usar")
                .requestAttr("usuarioAutenticado", funcionario))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isForbidden());
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .get("/api/respostas-reutilizaveis/" + restrita.getId())
                .requestAttr("usuarioAutenticado", funcionario))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isNotFound());
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .patch("/api/respostas-reutilizaveis/" + restrita.getId() + "/reutilizar")
                .requestAttr("usuarioAutenticado", funcionario))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isNotFound());
        assertThat(repository.findById(restrita.getId()).orElseThrow().getQuantidadeReutilizacoes()).isZero();
    }

    @Test void trataCuringasComoTextoLiteral() {
        resposta(1L, Set.of(PerfilUsuario.FUNCIONARIO));
        assertThat(service().buscarRespostasAnteriores(usuario(1L, PerfilUsuario.FUNCIONARIO), 1L, "%"))
                .isEmpty();
    }
}
