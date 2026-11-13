package br.com.hestia.configuracao.controller;

import br.com.hestia.autenticacao.contexto.EmpresaAtual;
import br.com.hestia.configuracao.dto.*;
import br.com.hestia.configuracao.repository.ConfiguracaoEmpresaRepository;
import br.com.hestia.configuracao.service.ConfiguracaoEmpresaService;
import jakarta.validation.Valid;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/empresas/{empresaId}/configuracao-inicial")
public class ConfiguracaoInicialController {
    private final EmpresaAtual empresa;
    private final JdbcTemplate jdbc;
    private final ConfiguracaoEmpresaRepository repository;
    private final ConfiguracaoEmpresaService service;

    public ConfiguracaoInicialController(EmpresaAtual empresa, JdbcTemplate jdbc,
            ConfiguracaoEmpresaRepository repository, ConfiguracaoEmpresaService service) {
        this.empresa = empresa; this.jdbc = jdbc; this.repository = repository; this.service = service;
    }
    public record Etapa(String nome, String estado, String destino) {}
    public record Situacao(List<Etapa> etapas, ConfiguracaoEmpresaResponseDTO configuracao) {}

    @GetMapping
    public Situacao consultar(@PathVariable Long empresaId) {
        empresa.validar(empresaId);
        var etapas = new ArrayList<Etapa>();
        etapas.add(etapa("Empresa cadastrada", contar("SELECT COUNT(*) FROM empresas WHERE id = ?", empresaId), "/administracao"));
        etapas.add(etapa("Departamento cadastrado", contar("SELECT COUNT(*) FROM departamentos WHERE empresa_id = ?", empresaId), "/administracao"));
        for (String perfil : List.of("ADMIN", "GESTOR", "FUNCIONARIO")) {
            long quantidade = jdbc.queryForObject("SELECT COUNT(*) FROM usuarios WHERE empresa_id = ? AND ativo = true AND perfil = ?", Long.class, empresaId, perfil);
            String nome = switch(perfil) { case "ADMIN" -> "Administrador ativo"; case "GESTOR" -> "Gestor ativo"; default -> "Colaborador ativo"; };
            etapas.add(etapa(nome, quantidade, "/administracao"));
        }
        etapas.add(etapa("Ferramenta aprovada", contar("SELECT COUNT(*) FROM ferramentas_ia WHERE empresa_id = ? AND status = 'APROVADA'", empresaId), "/administracao"));
        etapas.add(etapa("Política ativa", contar("SELECT COUNT(*) FROM politicas_uso WHERE empresa_id = ? AND ativa = true AND excluida = false", empresaId), "/governanca"));
        var configuracao = repository.findByEmpresaId(empresaId).map(ConfiguracaoEmpresaResponseDTO::from).orElse(null);
        etapas.add(new Etapa("Revisar configurações da organização", "REVISAR", "#parametros"));
        // O catálogo atual é fictício: não declarar o conteúdo do piloto como validado.
        etapas.add(etapa("Conteúdo publicado no catálogo da organização", contar("SELECT COUNT(*) FROM catalogo_treinamentos WHERE empresa_id=? AND estado='PUBLICADO'", empresaId), "/catalogo-real"));
        return new Situacao(List.copyOf(etapas), configuracao);
    }
    @PutMapping
    public ConfiguracaoEmpresaResponseDTO salvar(@PathVariable Long empresaId, @Valid @RequestBody ConfiguracaoEmpresaDTO dto) {
        empresa.validar(empresaId);
        return service.alterar(empresaId, dto);
    }
    private long contar(String sql, Long id) { return jdbc.queryForObject(sql, Long.class, id); }
    private Etapa etapa(String nome, long quantidade, String destino) {
        return new Etapa(nome, quantidade > 0 ? "CONCLUIDA" : "PENDENTE", destino);
    }
}
