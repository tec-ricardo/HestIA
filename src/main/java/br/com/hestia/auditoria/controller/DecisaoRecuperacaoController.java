package br.com.hestia.auditoria.controller;
import br.com.hestia.aprendizagem.AcessoCapacitacao;
import br.com.hestia.usuario.model.Usuario;
import org.springframework.web.bind.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
@RestController @RequestMapping("/auditoria/decisoes-recuperacao")
public class DecisaoRecuperacaoController {
    private final AcessoCapacitacao acesso;private final JdbcTemplate jdbc;
    public DecisaoRecuperacaoController(AcessoCapacitacao acesso,JdbcTemplate jdbc){this.acesso=acesso;this.jdbc=jdbc;}
    @GetMapping public Object consultar(@RequestAttribute("usuarioAutenticado")Usuario u){acesso.administrador(u);return jdbc.queryForList("SELECT * FROM decisoes_recuperacao WHERE empresa_id=? ORDER BY id DESC LIMIT 100",acesso.empresa(u));}
}
