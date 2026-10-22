package br.com.hestia.indicadores.maturidade;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.*;

@Service
public class AdaptadorOperacionalMaturidade {
    private final JdbcTemplate jdbc;private final CalculadorasMaturidade calculo;
    public AdaptadorOperacionalMaturidade(JdbcTemplate jdbc,CalculadorasMaturidade calculo){this.jdbc=jdbc;this.calculo=calculo;}
    public List<FatorCalculado> calcular(Long empresa,Long departamento,LocalDate inicio,LocalDate fim){
        List<FatorCalculado> fatores=new ArrayList<>();
        for(String codigo:List.of("aiLiteracy","governanca","produtividade","finOps","seguranca","colaboracao","desenvolvimentoCompetencias"))
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
        return fatores;
    }
}
