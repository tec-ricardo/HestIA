package br.com.hestia.aprendizagem;
import br.com.hestia.usuario.model.Usuario;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import static br.com.hestia.aprendizagem.AcessoCapacitacao.*;

@RestController @RequestMapping("/aprendizagem/conteudos")
public class ConteudoAprendizagemController {
    private final AvaliacaoAprendizagemService a;private final LeituraConteudoService leitura;private final AvaliacoesIniciaisHestiaService provas;
    public ConteudoAprendizagemController(AvaliacaoAprendizagemService a,LeituraConteudoService leitura,AvaliacoesIniciaisHestiaService provas){this.a=a;this.leitura=leitura;this.provas=provas;}
    public record Progresso(int etapa,String chave){}
    @GetMapping public Object listar(@RequestAttribute("usuarioAutenticado")Usuario u){a.acesso().empresa(u);provas.garantir(u);return a.conteudos().stream().filter(t->t.isAtivo()).map(t->consultar(u,t.getId())).toList();}
    @GetMapping("/{id}") public Object consultar(@RequestAttribute("usuarioAutenticado")Usuario u,@PathVariable Long id){
        a.acesso().empresa(u);provas.garantirCurso(u,id);var t=a.conteudo(id);String sha=hash(t.getConteudo());var progresso=a.jdbc().queryForList("SELECT etapa FROM aprendizagem_progresso WHERE empresa_id=? AND usuario_id=? AND treinamento_id=? AND conteudo_sha256=?",a.acesso().empresa(u),u.getId(),id,sha);int etapa=progresso.isEmpty()?0:((Number)progresso.getFirst().get("etapa")).intValue();
        var licoes=Arrays.stream(t.getConteudo().strip().split("\\n\\s*\\n")).filter(s->!s.isBlank()).map(s->{String texto=s.strip();int quebra=texto.indexOf("\n");String titulo=quebra>0&&quebra<100?texto.substring(0,quebra).strip():"Orientação prática";String corpo=quebra>0&&quebra<100?texto.substring(quebra+1).strip():texto;return Map.of("titulo",titulo,"texto",corpo);}).toList();
        var catalogos=a.catalogos(u,id).stream().filter(c->sha.equals(String.valueOf(c.get("conteudo_sha256")))).toList();var dto=new LinkedHashMap<String,Object>();dto.put("id",id);dto.put("catalogoPersistido",id>=1000);dto.put("titulo",t.getTitulo());dto.put("descricao",t.getDescricao());dto.put("categoria",t.getTema().name());dto.put("duracao",t.getDuracaoMinutos());dto.put("licoes",licoes);dto.put("etapa",etapa);dto.put("totalEtapas",licoes.size());// A conclusão exige aulas terminadas E avaliação aprovada da versão atual.
        boolean aulasConcluidas = etapa >= licoes.size();
        boolean avaliacaoAprovada = false;
        if (!catalogos.isEmpty()) {
            Long catalogoAtual = ((Number) catalogos.getFirst().get("id")).longValue();
            Long aprovadas = a.jdbc().queryForObject(
                    "SELECT COUNT(*) FROM aprendizagem_tentativas t " +
                            "JOIN aprendizagem_ciclos c ON c.id=t.ciclo_id " +
                            "WHERE c.empresa_id=? AND c.usuario_id=? AND c.catalogo_id=? " +
                            "AND t.status='APROVADA' AND t.finalizada_em IS NOT NULL",
                    Long.class, a.acesso().empresa(u), u.getId(), catalogoAtual);
            avaliacaoAprovada = aprovadas != null && aprovadas > 0;
        }
        dto.put("aulasConcluidas", aulasConcluidas);
        dto.put("avaliacaoAprovada", avaliacaoAprovada);
        dto.put("concluido", aulasConcluidas && avaliacaoAprovada);
        if(id>=1000){
            var metadados=a.jdbc().queryForList("SELECT id AS versaoConteudo,anterior_id,fonte,autorizacao_uso,objetivo,publico,atividade,criterio_aceite,publicado_em FROM catalogo_treinamentos WHERE id=? AND empresa_id=? AND estado='PUBLICADO'",id,a.acesso().empresa(u));
            if(!metadados.isEmpty())dto.put("metadados",metadados.getFirst());
        }
        dto.put("conteudoSha256",sha);dto.put("catalogoId",catalogos.isEmpty()?null:catalogos.getFirst().get("id"));dto.put("notaMinima",80);dto.put("limiteTentativas",3);return dto;
    }
    @PostMapping("/{id}/progresso") @Transactional
    public Object avancar(@RequestAttribute("usuarioAutenticado")Usuario u,@PathVariable Long id,@RequestBody Progresso dto){
        a.acesso().usuario(u,u.getId());a.acesso().serializar(u.getId());var t=a.conteudo(id);String sha=hash(t.getConteudo());int total=(int)Arrays.stream(t.getConteudo().strip().split("\\n\\s*\\n")).filter(s->!s.isBlank()).count();Long empresa=a.acesso().empresa(u);
        var rows=a.jdbc().queryForList("SELECT etapa FROM aprendizagem_progresso WHERE empresa_id=? AND usuario_id=? AND treinamento_id=? AND conteudo_sha256=?",empresa,u.getId(),id,sha);int atual=rows.isEmpty()?0:((Number)rows.getFirst().get("etapa")).intValue();exigir(dto.etapa()>=0&&dto.etapa()<=total&&dto.etapa()<=atual+1,HttpStatus.BAD_REQUEST,"Etapa inválida");int etapa=Math.max(atual,dto.etapa());
        if(rows.isEmpty())a.jdbc().update("INSERT INTO aprendizagem_progresso VALUES(?,?,?,?,?,?)",empresa,u.getId(),id,sha,etapa,java.time.LocalDateTime.now());else a.jdbc().update("UPDATE aprendizagem_progresso SET etapa=?,atualizado_em=? WHERE empresa_id=? AND usuario_id=? AND treinamento_id=? AND conteudo_sha256=?",etapa,java.time.LocalDateTime.now(),empresa,u.getId(),id,sha);
        if(etapa==total)leitura.concluir(u,id,"CONTEUDO:"+id+":"+sha);
        return Map.of("etapa",etapa,"totalEtapas",total);
    }
}
