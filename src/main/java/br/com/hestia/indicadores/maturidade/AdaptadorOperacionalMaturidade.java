package br.com.hestia.indicadores.maturidade;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.*;

@Service
public class AdaptadorOperacionalMaturidade {
    private final JdbcTemplate jdbc;private final CalculadorasMaturidade calculo;
    private final FonteEvidenciasSeguranca seguranca;
    public AdaptadorOperacionalMaturidade(JdbcTemplate jdbc, CalculadorasMaturidade calculo,
                                          FonteEvidenciasSeguranca seguranca) {
        this.jdbc = jdbc;
        this.calculo = calculo;
        this.seguranca = seguranca;
    }
    public List<FatorCalculado> calcular(Long empresa,Long departamento,LocalDate inicio,LocalDate fim){
        List<FatorCalculado> fatores=new ArrayList<>();
        for(String codigo:List.of("produtividade","finOps","colaboracao"))
            fatores.add(calculo.pendente(codigo,"SEM_DADOS","Fonte operacional compatível ainda não disponibilizada para esta fórmula","OPERACIONAL"));
        String escopo=" u.empresa_id = ? AND (CAST(? AS BIGINT) IS NULL OR u.departamento_id = ?) ";
        Object[] parametros={empresa,departamento,departamento,Timestamp.valueOf(inicio.atStartOfDay()),Timestamp.valueOf(fim.plusDays(1).atStartOfDay())};
        String periodoAvaliado=" AND r.data_hora >= ? AND r.data_hora < ? AND a.data_avaliacao >= ? AND a.data_avaliacao < ? ";
        Object[] parametrosAvaliacoes={empresa,departamento,departamento,parametros[3],parametros[4],parametros[3],parametros[4]};
        Long registros=jdbc.queryForObject("SELECT count(*) FROM registros_uso_ia r JOIN usuarios u ON u.id=r.usuario_id WHERE "+escopo+" AND r.data_hora >= ? AND r.data_hora < ?",Long.class,parametros);
        String preservado="""
            a.politica_versao_id IS NOT NULL AND v.empresa_id=u.empresa_id
            AND v.politica_uso_id=a.politica_uso_id AND a.regra_codigo IS NOT NULL
            AND a.regra_versao IS NOT NULL AND a.motivo_preservado IS NOT NULL
            AND a.regra_preservada IS NOT NULL AND a.limitacoes_preservadas IS NOT NULL
            """;
        var grupos=jdbc.queryForList("SELECT a.status, CASE WHEN "+preservado+" THEN 1 ELSE 0 END AS preservada, count(*) AS quantidade "
                +"FROM avaliacoes_conformidade a JOIN registros_uso_ia r ON r.id=a.registro_uso_id JOIN usuarios u ON u.id=r.usuario_id "
                +"LEFT JOIN politica_uso_versoes v ON v.id=a.politica_versao_id WHERE "+escopo+periodoAvaliado
                +"GROUP BY a.status, CASE WHEN "+preservado+" THEN 1 ELSE 0 END",parametrosAvaliacoes);
        long c=0,n=0,revisao=0,legadas=0;
        for(var grupo:grupos){long quantidade=((Number)grupo.get("quantidade")).longValue();
            if(((Number)grupo.get("preservada")).intValue()!=1){legadas+=quantidade;continue;}
            switch(String.valueOf(grupo.get("status"))){case "CONFORME"->c+=quantidade;case "NAO_CONFORME"->n+=quantidade;case "REQUER_REVISAO"->revisao+=quantidade;default->throw new IllegalStateException("Status operacional desconhecido");}
        }
        // O universo avaliado é restrito aos mesmos registros de uso no período do denominador.
        Long avaliados=jdbc.queryForObject("SELECT count(DISTINCT r.id) FROM registros_uso_ia r JOIN usuarios u ON u.id=r.usuario_id "
                +"JOIN avaliacoes_conformidade a ON a.registro_uso_id=r.id LEFT JOIN politica_uso_versoes v ON v.id=a.politica_versao_id "
                +"WHERE "+escopo+periodoAvaliado+" AND "+preservado,Long.class,parametrosAvaliacoes);
        fatores.add(calculo.usoResponsavel(c,n,revisao,legadas,registros==null?0:registros,avaliados==null?0:avaliados));
        fatores.add(seguranca.calcular(empresa, departamento, inicio, fim));
        fatores.add(governanca(parametros));
        fatores.add(competencias(parametros));
        var tentativas = new EvidenciasAiLiteracyRepository(jdbc).buscar(empresa, departamento, inicio, fim)
                .stream().map(t -> new SelecaoAiLiteracy.Tentativa(
                        t.tentativaId(), t.usuarioId(), t.avaliacaoId(), t.versaoAvaliacao(),
                        t.nota(), t.status(), t.finalizadaEm(), true)).toList();
        var selecao = SelecaoAiLiteracy.selecionar(tentativas, inicio, fim);
        fatores.add(calculo.literacyOperacional(selecao));
        return fatores;
    }

    private FatorCalculado governanca(Object[] parametros) {
        var totais = jdbc.queryForMap("""
                SELECT COUNT(*) AS usos, COUNT(h.status) AS classificados,
                       COUNT(CASE WHEN h.status = 'APROVADA' THEN 1 END) AS aprovados
                FROM registros_uso_ia r
                JOIN usuarios u ON u.id = r.usuario_id
                LEFT JOIN autorizacoes_ferramentas_ia h ON h.empresa_id = u.empresa_id
                    AND h.ferramenta_id = r.ferramenta_id AND h.inicio_vigencia <= r.data_hora
                    AND (h.fim_vigencia IS NULL OR h.fim_vigencia > r.data_hora)
                WHERE u.empresa_id = ? AND (CAST(? AS BIGINT) IS NULL OR u.departamento_id = ?)
                  AND r.data_hora >= ? AND r.data_hora < ?
                """, parametros);
        return calculo.governancaOperacional(
                ((Number) totais.get("usos")).longValue(),
                ((Number) totais.get("classificados")).longValue(),
                ((Number) totais.get("aprovados")).longValue());
    }

    private FatorCalculado competencias(Object[] parametros) {
        var totais = jdbc.queryForMap("""
                WITH coorte AS (
                    SELECT id, empresa_id FROM usuarios
                    WHERE empresa_id = ? AND (CAST(? AS BIGINT) IS NULL OR departamento_id = ?)
                      AND ativo = TRUE
                ), resultados AS (
                    SELECT u.id AS usuario_id, t.nota, t.status,
                           CASE WHEN treino.id IS NOT NULL AND t.nota IS NOT NULL
                                AND EXISTS (
                                    SELECT 1 FROM aprendizagem_leituras l
                                    JOIN conclusoes_microtreinamento cm ON cm.id = l.conclusao_id
                                    WHERE l.empresa_id = u.empresa_id AND l.usuario_id = u.id
                                      AND l.treinamento_id = a.treinamento_id
                                      AND l.conteudo_sha256 = a.conteudo_sha256
                                      AND cm.usuario_id = u.id AND cm.microtreinamento_id = a.treinamento_id
                                      AND cm.concluido = TRUE AND cm.data_conclusao <= l.concluida_em
                                      AND l.concluida_em <= t.finalizada_em
                                )
                                AND ((t.status = 'APROVADA' AND t.nota >= 80)
                                  OR (t.status = 'REPROVADA' AND t.nota < 80))
                                THEN 1 ELSE 0 END AS valida
                    FROM coorte u
                    JOIN aprendizagem_ciclos c ON c.usuario_id = u.id AND c.empresa_id = u.empresa_id
                    JOIN aprendizagem_tentativas t ON t.ciclo_id = c.id
                    LEFT JOIN aprendizagem_catalogo a ON a.id = c.catalogo_id AND a.empresa_id = u.empresa_id
                    LEFT JOIN catalogo_treinamentos treino ON treino.id = a.treinamento_id
                        AND treino.empresa_id = u.empresa_id AND treino.conteudo_sha256 = a.conteudo_sha256
                        AND treino.tema = a.tema AND a.publicado_em <= t.finalizada_em
                        AND treino.publicado_em <= t.finalizada_em
                        AND (treino.estado = 'PUBLICADO'
                            OR (treino.estado = 'RETIRADO' AND treino.retirado_em > t.finalizada_em))
                    WHERE t.finalizada_em >= ? AND t.finalizada_em < ?
                      AND t.status IN ('APROVADA', 'REPROVADA')
                )
                SELECT (SELECT COUNT(*) FROM coorte) AS pessoas,
                       COUNT(*) AS candidatas,
                       COUNT(CASE WHEN valida = 1 THEN 1 END) AS validas,
                       COUNT(DISTINCT CASE WHEN valida = 1 THEN usuario_id END) AS conhecidas,
                       COUNT(DISTINCT CASE WHEN valida = 1 AND status = 'APROVADA' THEN usuario_id END) AS aprovadas
                FROM resultados
                """, parametros);
        return calculo.competenciasOperacionais(
                ((Number) totais.get("aprovadas")).longValue(),
                ((Number) totais.get("conhecidas")).longValue(),
                ((Number) totais.get("pessoas")).longValue(),
                ((Number) totais.get("validas")).longValue(),
                ((Number) totais.get("candidatas")).longValue());
    }
}
