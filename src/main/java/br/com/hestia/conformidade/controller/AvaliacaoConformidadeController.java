package br.com.hestia.conformidade.controller;

import br.com.hestia.politica.dto.PoliticaUsoVersaoDTO;
import br.com.hestia.conformidade.dto.AvaliacaoConformidadeDTO;
import br.com.hestia.conformidade.dto.AvaliacaoConformidadeResponseDTO;
import br.com.hestia.conformidade.service.AvaliacaoConformidadeService;
import br.com.hestia.autenticacao.contexto.EmpresaAtual;
import br.com.hestia.usuario.model.PerfilUsuario;
import br.com.hestia.usuario.model.Usuario;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.Map;

@RestController
@RequestMapping("/avaliacoes-conformidade")
public class AvaliacaoConformidadeController {
    private final AvaliacaoConformidadeService service;
    private final JdbcTemplate jdbc;
    private final EmpresaAtual empresaAtual;

    public AvaliacaoConformidadeController(AvaliacaoConformidadeService service, JdbcTemplate jdbc,
                                           EmpresaAtual empresaAtual) {
        this.service = service;
        this.jdbc = jdbc;
        this.empresaAtual = empresaAtual;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AvaliacaoConformidadeResponseDTO avaliar(@Valid @RequestBody AvaliacaoConformidadeDTO dto) {
        return service.avaliar(dto);
    }


    @GetMapping("/{id}/politica")
    public PoliticaUsoVersaoDTO politicaHistorica(@PathVariable Long id) {
        return service.buscarPoliticaHistorica(id);
    }

    @GetMapping("/{id}")
    public AvaliacaoConformidadeResponseDTO buscar(@PathVariable Long id) {
        return service.buscar(id);
    }

    @GetMapping("/resumo")
    public Map<String, Object> resumo(@RequestAttribute("usuarioAutenticado") Usuario usuario) {
        boolean pessoal = usuario.getPerfil() == PerfilUsuario.FUNCIONARIO;
        String filtroUsuario = pessoal ? " AND u.id = ?" : "";
        Object[] parametros = pessoal
                ? new Object[]{empresaAtual.obterId(), usuario.getId()}
                : new Object[]{empresaAtual.obterId()};
        String base = " FROM registros_uso_ia r JOIN usuarios u ON u.id=r.usuario_id "
                + "LEFT JOIN avaliacoes_conformidade a ON a.registro_uso_id=r.id "
                + "WHERE u.empresa_id=?" + filtroUsuario;
        Map<String, Object> contagens = jdbc.queryForMap("SELECT count(*) registros, "
                + "count(a.id) avaliadas, "
                + "count(a.id) FILTER (WHERE a.status='CONFORME') conformes, "
                + "count(a.id) FILTER (WHERE a.status='NAO_CONFORME') nao_conformes, "
                + "count(a.id) FILTER (WHERE a.status='REQUER_REVISAO') requerem_revisao" + base,
                parametros);
        long avaliadas = ((Number) contagens.get("avaliadas")).longValue();
        long conformes = ((Number) contagens.get("conformes")).longValue();
        contagens.put("percentualConformidade", avaliadas == 0 ? null
                : Math.round(conformes * 10000.0 / avaliadas) / 100.0);
        return contagens;
    }
}
