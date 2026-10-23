package br.com.hestia.aprendizagem;
import br.com.hestia.usuario.model.Usuario;
import org.springframework.web.bind.annotation.*;
import java.util.*;
import static br.com.hestia.aprendizagem.ContratoAvaliacao.*;

@RestController @org.springframework.transaction.annotation.Transactional
public class AprendizagemController {
    private final AvaliacaoAprendizagemService a;private final LeituraConteudoService leitura;private final CertificacaoService certificacoes;
    public AprendizagemController(AvaliacaoAprendizagemService a,LeituraConteudoService leitura,CertificacaoService certificacoes){this.a=a;this.leitura=leitura;this.certificacoes=certificacoes;}
    public record Confirmacao(String chave){}
    @PostMapping("/aprendizagem/catalogo") public Map<String,Long> publicar(@RequestAttribute("usuarioAutenticado")Usuario u,@RequestBody Publicacao p){return Map.of("id",a.publicar(u,p));}
    @GetMapping("/aprendizagem/microtreinamentos/{id}") public Object catalogo(@RequestAttribute("usuarioAutenticado")Usuario u,@PathVariable Long id){return a.catalogos(u,id);}
    @PostMapping("/aprendizagem/microtreinamentos/{id}/conclusao") public Object concluir(@RequestAttribute("usuarioAutenticado")Usuario u,@PathVariable Long id,@RequestBody Confirmacao dto){return Map.of("leituraId",leitura.concluir(u,id,dto.chave()));}
    @PostMapping("/aprendizagem/catalogo/{id}/tentativas") public Tentativa abrir(@RequestAttribute("usuarioAutenticado")Usuario u,@PathVariable Long id){return a.abrir(u,id);}
    @GetMapping("/aprendizagem/tentativas/{id}") public Tentativa consultar(@RequestAttribute("usuarioAutenticado")Usuario u,@PathVariable Long id){return a.consultar(u,id);}
    @PostMapping("/aprendizagem/tentativas/{id}/respostas") public Tentativa responder(@RequestAttribute("usuarioAutenticado")Usuario u,@PathVariable Long id,@RequestBody Envio envio){return a.enviar(u,id,envio);}
    @GetMapping("/aprendizagem/usuarios/{id}/historico") public Object historico(@RequestAttribute("usuarioAutenticado")Usuario u,@PathVariable Long id){return a.historico(u,id);}
    @GetMapping("/aprendizagem/usuarios/{id}/ai-literacy") public Object literacy(@RequestAttribute("usuarioAutenticado")Usuario autor,@PathVariable Long id){
        var usuario=a.acesso().usuario(autor,id);Long empresa=a.acesso().empresa(usuario);
        int publicados=a.jdbc().queryForObject("SELECT count(*) FROM aprendizagem_catalogo WHERE empresa_id=?",Integer.class,empresa);
        var notas=a.jdbc().queryForList("""
            SELECT c.catalogo_id,max(t.nota) AS nota
              FROM aprendizagem_ciclos c JOIN aprendizagem_tentativas t ON t.ciclo_id=c.id
             WHERE c.empresa_id=? AND c.usuario_id=? AND t.finalizada_em IS NOT NULL AND t.nota IS NOT NULL
             GROUP BY c.catalogo_id ORDER BY c.catalogo_id
            """,empresa,usuario.getId());
        int avaliados=notas.size();double media=notas.stream().mapToDouble(x->((Number)x.get("nota")).doubleValue()).average().orElse(Double.NaN);
        long aprovados=notas.stream().filter(x->((Number)x.get("nota")).doubleValue()>=80).count();
        int leituras=a.jdbc().queryForObject("SELECT count(DISTINCT treinamento_id) FROM aprendizagem_leituras WHERE empresa_id=? AND usuario_id=?",Integer.class,empresa,usuario.getId());
        var resposta=new LinkedHashMap<String,Object>();resposta.put("usuarioId",usuario.getId());resposta.put("conteudosPublicados",publicados);resposta.put("conteudosLidos",leituras);resposta.put("avaliacoesConcluidas",avaliados);resposta.put("avaliacoesAprovadas",aprovados);
        resposta.put("notaMedia",Double.isNaN(media)?null:Math.round(media*100.0)/100.0);resposta.put("cobertura",publicados==0?null:Math.round(avaliados*10000.0/publicados)/100.0);
        resposta.put("nivel",Double.isNaN(media)?"NAO_AVALIADO":media<60?"INICIAL":media<80?"EM_DESENVOLVIMENTO":media<90?"COMPETENTE":"AVANCADO");resposta.put("versaoRegra","AI-LITERACY-APRENDIZAGEM-1.0");
        return resposta;
    }
    @PostMapping("/aprendizagem/ciclos/{id}/liberacao") public Object liberar(@RequestAttribute("usuarioAutenticado")Usuario u,@PathVariable Long id,@RequestBody Liberacao dto){return Map.of("cicloId",a.liberar(u,id,dto));}
    @PutMapping("/aprendizagem/permissoes/{id}") public void permissao(@RequestAttribute("usuarioAutenticado")Usuario u,@PathVariable Long id){a.acesso().administrador(u);a.acesso().usuario(u,id);a.acesso().serializar(id);if(a.jdbc().queryForObject("SELECT count(*) FROM permissoes_capacitacao WHERE empresa_id=? AND usuario_id=?",Long.class,a.acesso().empresa(u),id)==0)a.jdbc().update("INSERT INTO permissoes_capacitacao VALUES(?,?,?,?)",a.acesso().empresa(u),id,u.getId(),java.time.LocalDateTime.now());}
    @DeleteMapping("/aprendizagem/permissoes/{id}") public void revogar(@RequestAttribute("usuarioAutenticado")Usuario u,@PathVariable Long id){a.acesso().administrador(u);a.acesso().usuario(u,id);a.jdbc().update("DELETE FROM permissoes_capacitacao WHERE empresa_id=? AND usuario_id=?",a.acesso().empresa(u),id);}
    @PostMapping("/certificacoes/definicoes") public Object publicarCertificacao(@RequestAttribute("usuarioAutenticado")Usuario u,@RequestBody CertificacaoService.Publicacao p){return Map.of("id",certificacoes.publicar(u,p));}
    @PostMapping("/certificacoes/usuarios/{id}/verificar") public Object verificar(@RequestAttribute("usuarioAutenticado")Usuario u,@PathVariable Long id){return certificacoes.verificar(u,id);}
    @GetMapping("/certificacoes/usuarios/{id}") public Object certificacoes(@RequestAttribute("usuarioAutenticado")Usuario u,@PathVariable Long id){return certificacoes.historico(u,id);}
}
