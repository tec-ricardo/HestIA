package br.com.hestia.gamificacao.controller;

import br.com.hestia.usuario.model.Usuario;
import br.com.hestia.usuario.model.PerfilUsuario;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@RestController
@RequestMapping("/gamificacao")
public class DetalhamentoXpController {
    private final JdbcTemplate jdbc;
    public DetalhamentoXpController(JdbcTemplate jdbc) { this.jdbc=jdbc; }

    @GetMapping("/usuarios/{id}/detalhamento-xp")
    @Transactional(readOnly=true)
    public Map<String,Object> detalhar(@PathVariable Long id, @RequestAttribute("usuarioAutenticado") Usuario autor) {
        if(autor==null || autor.getEmpresa()==null || autor.getEmpresa().getId()==null || autor.getPerfil()==null)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        Long empresa=autor.getEmpresa().getId(), departamento=null;
        if(autor.getPerfil()==PerfilUsuario.GESTOR){
            var d=autor.getDepartamento();
            if(d==null||d.getId()==null||d.getEmpresa()==null||!empresa.equals(d.getEmpresa().getId()))
                throw new ResponseStatusException(HttpStatus.FORBIDDEN);
            departamento=d.getId();
        }
        boolean outroColaborador=autor.getPerfil()==PerfilUsuario.FUNCIONARIO&&!Objects.equals(autor.getId(),id);
        var pessoas=jdbc.queryForList("""
            SELECT u.nome, COALESCE(p.xp_total,0) AS xp
            FROM usuarios u LEFT JOIN progressos_usuario p ON p.usuario_id=u.id
            WHERE u.id=? AND u.empresa_id=? AND u.ativo=TRUE
              AND (CAST(? AS BIGINT) IS NULL OR u.departamento_id=?)
              AND (?=FALSE OR p.usuario_id IS NOT NULL)
            """,id,empresa,departamento,departamento,outroColaborador);
        if(pessoas.isEmpty())throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        var itens=jdbc.queryForList("""
            SELECT m.acao, COUNT(*) AS quantidade, SUM(m.pontos) AS xp
            FROM movimentos_experiencia m JOIN usuarios u ON u.id=m.usuario_id
            WHERE u.id=? AND u.empresa_id=?
            GROUP BY m.acao ORDER BY SUM(m.pontos) DESC
            """,id,empresa);
        // Apenas totais agrupados, jamais referências internas, prompts ou datas de colegas.
        long total=itens.stream().mapToLong(i->((Number)i.get("xp")).longValue()).sum();
        long saldo=((Number)pessoas.getFirst().get("xp")).longValue();
        return Map.of("nome",pessoas.getFirst().get("nome"),"itens",itens,"xpTotal",saldo,
            "xpMovimentos",total,"conciliado",total==saldo);
    }
}
