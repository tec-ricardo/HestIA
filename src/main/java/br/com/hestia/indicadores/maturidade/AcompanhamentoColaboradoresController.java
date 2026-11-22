package br.com.hestia.indicadores.maturidade;

import br.com.hestia.usuario.model.Usuario;
import br.com.hestia.usuario.model.PerfilUsuario;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.sql.Timestamp;
import java.math.BigDecimal;
import java.util.*;

@RestController
@RequestMapping("/gestao")
public class AcompanhamentoColaboradoresController {
    private final JdbcTemplate jdbc;
    private final EvidenciasAiLiteracyRepository evidencias;

    public AcompanhamentoColaboradoresController(JdbcTemplate jdbc, EvidenciasAiLiteracyRepository evidencias) {
        this.jdbc = jdbc;
        this.evidencias = evidencias;
    }

    static Long departamentoAutorizado(Usuario autor) {
        if (autor == null || autor.getEmpresa() == null || autor.getEmpresa().getId() == null
                || (autor.getPerfil() != PerfilUsuario.ADMIN && autor.getPerfil() != PerfilUsuario.GESTOR))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        if (autor.getPerfil() == PerfilUsuario.ADMIN) return null;
        var d = autor.getDepartamento();
        if (d == null || d.getId() == null || d.getEmpresa() == null
                || !autor.getEmpresa().getId().equals(d.getEmpresa().getId()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        return d.getId();
    }

    @GetMapping("/colaboradores")
    @Transactional(readOnly = true)
    public Map<String,Object> listar(@RequestAttribute("usuarioAutenticado") Usuario autor) {
        Long dep = departamentoAutorizado(autor);
        Long empresa = autor.getEmpresa().getId();
        LocalDate fim = LocalDate.now(), inicio = fim.minusDays(29);
        Timestamp de = Timestamp.valueOf(inicio.atStartOfDay());
        Timestamp ate = Timestamp.valueOf(fim.plusDays(1).atStartOfDay());
        var pessoas = jdbc.queryForList("""
            SELECT u.id, u.nome, d.nome AS departamento, COALESCE(p.xp_total,0) AS xp
            FROM usuarios u JOIN departamentos d ON d.id=u.departamento_id AND d.empresa_id=u.empresa_id
            LEFT JOIN progressos_usuario p ON p.usuario_id=u.id
            WHERE u.empresa_id=? AND u.ativo=TRUE AND u.perfil='FUNCIONARIO'
              AND (CAST(? AS BIGINT) IS NULL OR u.departamento_id=?)
            ORDER BY u.nome
            """, empresa, dep, dep);
        var tentativas = evidencias.buscar(empresa,dep,inicio,fim).stream()
            .map(t -> new SelecaoAiLiteracy.Tentativa(t.tentativaId(),t.usuarioId(),t.avaliacaoId(),
                t.versaoAvaliacao(),t.nota(),t.status(),t.finalizadaEm(),true)).toList();
        var selecao = SelecaoAiLiteracy.selecionar(tentativas,inicio,fim);
        List<Map<String,Object>> colaboradores = new ArrayList<>();
        for(var pessoa:pessoas) {
            Map<String,Object> c = new LinkedHashMap<>(pessoa);
            long id = ((Number)c.get("id")).longValue();
            c.putAll(jdbc.queryForMap("""
                SELECT COUNT(*) AS utilizacoes, COALESCE(SUM(r.tokens_total),0) AS tokens,
                    COUNT(CASE WHEN r.tokens_total>0 THEN 1 END) AS usos_com_tokens,
                    COUNT(CASE WHEN r.status_execucao='SUCESSO' THEN 1 END) AS sucessos
                FROM registros_uso_ia r JOIN usuarios u ON u.id=r.usuario_id
                WHERE u.id=? AND u.empresa_id=? AND r.data_hora>=? AND r.data_hora<?
                """, id,empresa,de,ate));
            c.putAll(jdbc.queryForMap("""
                SELECT COUNT(CASE WHEN a.status='CONFORME' THEN 1 END) AS conformes,
                    COUNT(CASE WHEN a.status='NAO_CONFORME' THEN 1 END) AS nao_conformes,
                    COUNT(CASE WHEN a.status='REQUER_REVISAO' THEN 1 END) AS revisoes
                FROM avaliacoes_conformidade a JOIN registros_uso_ia r ON r.id=a.registro_uso_id
                JOIN usuarios u ON u.id=r.usuario_id JOIN politica_uso_versoes v
                  ON v.id=a.politica_versao_id AND v.empresa_id=u.empresa_id AND v.politica_uso_id=a.politica_uso_id
                WHERE u.id=? AND u.empresa_id=? AND r.data_hora>=? AND r.data_hora<?
                  AND a.data_avaliacao>=? AND a.data_avaliacao<?
                  AND a.regra_codigo IS NOT NULL AND a.regra_versao IS NOT NULL
                  AND a.motivo_preservado IS NOT NULL AND a.regra_preservada IS NOT NULL
                  AND a.limitacoes_preservadas IS NOT NULL
                """, id,empresa,de,ate,de,ate));
            long conformes=((Number)c.get("conformes")).longValue();
            long nao=((Number)c.get("nao_conformes")).longValue();
            c.put("totalAvaliado",conformes+nao);
            c.put("taxaConformidade",conformes+nao==0?null:100.0*conformes/(conformes+nao));
            List<BigDecimal> notas=selecao.notasPorPessoa().getOrDefault(Long.toString(id),List.of());
            c.put("aiLiteracy",notas.isEmpty()?null:notas.stream().mapToDouble(BigDecimal::doubleValue).average().orElseThrow());
            c.put("amostraLiteracy",notas.size());
            c.put("avaliacoesQualificadas",notas.stream().filter(n->n.compareTo(BigDecimal.valueOf(80))>=0).count());
            c.putAll(jdbc.queryForMap("""
                SELECT COUNT(CASE WHEN m.acao='REUTILIZACAO_RESPOSTA' THEN 1 END) AS reutilizacoes,
                    COUNT(CASE WHEN m.acao='MICRO_TREINAMENTO' THEN 1 END) AS treinamentos,
                    COALESCE(SUM(m.pontos),0) AS xp_periodo
                FROM movimentos_experiencia m JOIN usuarios u ON u.id=m.usuario_id
                WHERE u.id=? AND u.empresa_id=? AND m.data_hora>=? AND m.data_hora<?
                """,id,empresa,de,ate));
            c.put("semFonte",jdbc.queryForObject("""
                SELECT COUNT(*) FROM interacoes_rag WHERE empresa_id=? AND usuario_id=?
                    AND criada_em>=? AND criada_em<? AND estado='SEM_CONTEUDO_AUTORIZADO'
                """,Long.class,empresa,id,de,ate));
            colaboradores.add(c);
        }
        Map<String,Object> resposta=new LinkedHashMap<>();
        resposta.put("empresaId",empresa);
        resposta.put("empresa",autor.getEmpresa().getNome());
        resposta.put("departamento",dep==null?"Empresa inteira":autor.getDepartamento().getNome());
        resposta.put("inicio",inicio); resposta.put("fim",fim);
        resposta.put("fonte","OPERACIONAL");
        resposta.put("colaboradores",colaboradores);
        resposta.put("departamentos",jdbc.queryForObject("SELECT COUNT(*) FROM departamentos WHERE empresa_id=? AND (CAST(? AS BIGINT) IS NULL OR id=?)",Long.class,empresa,dep,dep));
        resposta.put("politicas",jdbc.queryForObject("SELECT COUNT(*) FROM politicas_uso WHERE empresa_id=? AND excluida=FALSE",Long.class,empresa));
        resposta.put("ferramentas",jdbc.queryForObject("SELECT COUNT(*) FROM ferramentas_ia WHERE empresa_id=?",Long.class,empresa));
        return resposta;
    }
}
