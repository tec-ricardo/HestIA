package br.com.hestia.registro.controller;
import br.com.hestia.usuario.model.Usuario;
import br.com.hestia.aprendizagem.AcessoCapacitacao;
import br.com.hestia.ferramenta.repository.FerramentaIARepository;
import org.springframework.web.bind.annotation.*;
import java.util.*;
@RestController
public class ContextoRegistroController {
    private final AcessoCapacitacao acesso;private final FerramentaIARepository ferramentas;
    public ContextoRegistroController(AcessoCapacitacao acesso,FerramentaIARepository ferramentas){this.acesso=acesso;this.ferramentas=ferramentas;}
    @GetMapping("/registros-uso-ia/contexto") public Object contexto(@RequestAttribute("usuarioAutenticado")Usuario u){Long empresa=acesso.empresa(u);return Map.of("usuarioId",u.getId(),"empresaId",empresa,"origem","MANUAL","ferramentas",ferramentas.findByEmpresaId(empresa).stream().map(f->Map.of("id",f.getId(),"nome",f.getNome())).toList(),"aviso","Dados não observados devem ser informados; registros manuais não representam monitoramento completo de uso externo.");}
}
