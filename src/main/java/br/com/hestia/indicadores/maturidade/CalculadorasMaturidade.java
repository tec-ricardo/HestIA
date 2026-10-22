package br.com.hestia.indicadores.maturidade;

import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.math.MathContext;
import java.util.*;

/** Fórmulas da proposta 0.1; índices descritivos não são comprovação de causalidade. */
@Component
public class CalculadorasMaturidade {
    public static final String VERSAO = "proposta-0.1";
    private static final MathContext MC = MathContext.DECIMAL128;
    private static final BigDecimal CEM = new BigDecimal("100");
    private static final BigDecimal COBERTURA = new BigDecimal("0.95");
    private static final Map<String, BigDecimal> PESOS = Map.of(
        "aiLiteracy", new BigDecimal("0.15"), "governanca", new BigDecimal("0.15"),
        "usoResponsavel", new BigDecimal("0.15"), "produtividade", new BigDecimal("0.10"),
        "finOps", new BigDecimal("0.10"), "seguranca", new BigDecimal("0.20"),
        "colaboracao", new BigDecimal("0.05"), "desenvolvimentoCompetencias", new BigDecimal("0.10"));

    public static BigDecimal razao(BigDecimal n, BigDecimal d) {
        return d.signum() == 0 ? null : n.divide(d, MC);
    }
    public static BigDecimal razao(long n, long d) { return razao(BigDecimal.valueOf(n), BigDecimal.valueOf(d)); }
    private static BigDecimal percentual(long n,long d) {
        var r=razao(n,d); return r==null?null:r.multiply(CEM);
    }
    private static void contagens(long... valores) {
        for(long v:valores) if(v<0) throw new IllegalArgumentException("Contagem negativa");
    }
    private static boolean suficiente(BigDecimal c) { return c!=null && c.compareTo(COBERTURA)>=0; }
    private FatorCalculado fator(String codigo,BigDecimal nota,BigDecimal tecnica,BigDecimal classificacao,
            long amostra,Long populacao,BigDecimal n,BigDecimal d,String formula,String fonte,
            boolean requisito,Map<String,BigDecimal> stats,String... avisos) {
        boolean pequeno=codigo.equals("finOps")||codigo.equals("seguranca");
        boolean elegivel=nota!=null&&suficiente(tecnica)&&suficiente(classificacao)
                &&(pequeno?amostra>0:amostra>=30)&&requisito;
        return new FatorCalculado(codigo,PESOS.get(codigo),nota,nota==null?"SEM_DADOS":elegivel?"CALCULADO":"PARCIAL",
            elegivel,elegivel?"Critérios da proposta atendidos":nota==null?"Fonte ou denominador sem observações válidas":"Cobertura, amostra ou evidência insuficiente para o global",
            tecnica,classificacao,amostra,populacao,n,d,formula,fonte,stats,List.of(avisos),VERSAO);
    }
    public FatorCalculado pendente(String codigo,String estado,String motivo,String fonte) {
        if(!PESOS.containsKey(codigo))throw new IllegalArgumentException("Fator desconhecido");
        return new FatorCalculado(codigo,PESOS.get(codigo),null,estado,false,motivo,null,null,0,null,
                null,null,"Não executada",fonte,Map.of(),List.of(),VERSAO);
    }
    public FatorCalculado literacy(Map<String,List<BigDecimal>> scores,long candidatas) {
        contagens(candidatas);BigDecimal soma=BigDecimal.ZERO;long pessoas=0,validas=0;
        for(var valores:scores.values()) {
            if(valores.isEmpty()) continue;
            BigDecimal total=BigDecimal.ZERO;
            for(var v:valores){if(v==null||v.signum()<0||v.compareTo(CEM)>0)throw new IllegalArgumentException("Score inválido");total=total.add(v);}
            validas+=valores.size();pessoas++;soma=soma.add(total.divide(BigDecimal.valueOf(valores.size()),MC));
        }
        if(validas>candidatas)throw new IllegalArgumentException("Amostra supera candidatas");
        return fator("aiLiteracy",pessoas==0?null:soma.divide(BigDecimal.valueOf(pessoas),MC),razao(validas,candidatas),
            BigDecimal.ONE,pessoas,pessoas,null,null,"Média das médias por pessoa","training_records",true,
            Map.of("conclusoesValidas",BigDecimal.valueOf(validas)),"Representa pessoas observadas; não todos os empregados");
    }
    public FatorCalculado governanca(BigDecimal q,BigDecimal k,BigDecimal a,long observacoes,
            boolean volumeConhecido,boolean historicoTemporal,BigDecimal tecnica) {
        if(q.signum()<0||k.signum()<0||a.signum()<0||a.compareTo(k)>0||k.compareTo(q)>0)throw new IllegalArgumentException("Volumes inconsistentes");
        if(!volumeConhecido)return pendente("governanca","DADOS_INVALIDOS","request_count inválido torna a cobertura ponderada desconhecida","ai_usage_logs / approved_ai_tools");
        var r=razao(a,k);
        return fator("governanca",r==null?null:r.multiply(CEM),tecnica,razao(k,q),observacoes,observacoes,a,k,
                "100 * requests aprovados / requests classificados","ai_usage_logs / approved_ai_tools",historicoTemporal,
                Map.of("requestsCandidatos",q),"Cadastro observado não comprova histórico de revogações");
    }
    public FatorCalculado usoResponsavel(long c,long n,long revisao,long legadas,long registrosElegiveis,long registrosAvaliados) {
        contagens(c,n,revisao,legadas,registrosElegiveis,registrosAvaliados);
        long preservadas=c+n+revisao;
        return fator("usoResponsavel",percentual(c,c+n),razao(preservadas,preservadas+legadas),razao(c+n,preservadas),
            preservadas,registrosElegiveis,BigDecimal.valueOf(c),BigDecimal.valueOf(c+n),"100 * C / (C + N)",
            "avaliacoes_conformidade / registros_uso_ia",suficiente(razao(registrosAvaliados,registrosElegiveis)),
            registrosElegiveis==0?Map.of("avaliacoesLegadas",BigDecimal.valueOf(legadas)):Map.of("avaliacoesLegadas",BigDecimal.valueOf(legadas),"coberturaAvaliacao",razao(registrosAvaliados,registrosElegiveis)),
            "REQUER_REVISAO não é aprovação nem reprovação; legadas não compõem a nota");
    }
    public FatorCalculado produtividade(List<BigDecimal> duracoes,long candidatas,BigDecimal metaDias) {
        contagens(candidatas);if(duracoes.size()>candidatas)throw new IllegalArgumentException("Amostra inválida");
        var ordenadas=new ArrayList<>(duracoes);for(var d:ordenadas)if(d==null||d.signum()<0)throw new IllegalArgumentException("Duração inválida");
        ordenadas.sort(Comparator.naturalOrder());
        BigDecimal mediana=ordenadas.isEmpty()?null:ordenadas.size()%2==1?ordenadas.get(ordenadas.size()/2):
            ordenadas.get(ordenadas.size()/2-1).add(ordenadas.get(ordenadas.size()/2)).divide(new BigDecimal("2"),MC);
        if(metaDias==null) {
            var p=pendente("produtividade","CONFIGURACAO_PENDENTE","Meta de dias por tipo de issue/escopo não configurada","issues / projects");
            return new FatorCalculado(p.codigo(),p.peso(),null,p.estado(),false,p.motivo(),razao(duracoes.size(),candidatas),null,
                duracoes.size(),candidatas,null,null,"min(100, 100 * T / medianaDias)",p.fonte(),mediana==null?Map.of():Map.of("medianaDias",mediana),p.avisos(),VERSAO);
        }
        if(metaDias.signum()<=0)throw new IllegalArgumentException("Meta deve ser positiva");
        BigDecimal nota=mediana==null?null:mediana.signum()==0?CEM:CEM.multiply(metaDias).divide(mediana,MC).min(CEM);
        return fator("produtividade",nota,razao(duracoes.size(),candidatas),BigDecimal.ONE,duracoes.size(),candidatas,null,null,
            "min(100, 100 * T / medianaDias)","issues / projects",true,mediana==null?Map.of("metaDias",metaDias):Map.of("metaDias",metaDias,"medianaDias",mediana),
            "Aderência ao processo; não mede efeito causal da IA");
    }
    public FatorCalculado finOps(BigDecimal orcamento,BigDecimal custo,boolean moedaResolvida,boolean coberturaCompleta,long centrosMeses) {
        if(!moedaResolvida)return pendente("finOps","CONFIGURACAO_PENDENTE","Moeda autorizada dos orçamentos/conversão não configurada","ai_billing / ai_budgets");
        if(orcamento==null||orcamento.signum()<=0||custo==null||custo.signum()<0||!coberturaCompleta||centrosMeses<=0)
            return pendente("finOps","DADOS_INVALIDOS","Orçamento, custo observado ou cobertura de meses/centros inválidos","ai_billing / ai_budgets");
        var nota=CEM.multiply(new BigDecimal("2").subtract(custo.divide(orcamento,MC))).max(BigDecimal.ZERO).min(CEM);
        return fator("finOps",nota,BigDecimal.ONE,BigDecimal.ONE,centrosMeses,centrosMeses,custo,orcamento,
            "limitar(100 * (2 - C / B), 0, 100)","ai_billing / ai_budgets",true,Map.of(),"Aderência ao orçamento não comprova eficiência ou ROI");
    }
    public FatorCalculado seguranca(long aprovados,long avaliados,long obrigatorios) {
        contagens(aprovados,avaliados,obrigatorios);if(aprovados>avaliados||avaliados>obrigatorios)throw new IllegalArgumentException("Controles inconsistentes");
        return fator("seguranca",percentual(aprovados,avaliados),razao(avaliados,obrigatorios),BigDecimal.ONE,avaliados,obrigatorios,
            BigDecimal.valueOf(aprovados),BigDecimal.valueOf(avaliados),"100 * P / E","Catálogo de controles e evidências",avaliados==obrigatorios,
            Map.of(),"Ausência de incidentes não é evidência de controle eficaz");
    }
    public FatorCalculado colaboracao(long revisados,long validos,long candidatos) {
        contagens(revisados,validos,candidatos);if(revisados>validos||validos>candidatos)throw new IllegalArgumentException("PRs inconsistentes");
        return fator("colaboracao",percentual(revisados,validos),razao(validos,candidatos),BigDecimal.ONE,validos,candidatos,
            BigDecimal.valueOf(revisados),BigDecimal.valueOf(validos),"100 * PRs com revisão / PRs válidos","pull_requests / projects",true,Map.of(),
            "A origem não identifica o revisor; revisão independente não comprovada");
    }
    public FatorCalculado competencias(long aprovados,long conhecidos,long pessoas,long linhasValidas,long candidatas) {
        contagens(aprovados,conhecidos,pessoas,linhasValidas,candidatas);
        if(aprovados>conhecidos||conhecidos>pessoas||linhasValidas>candidatas)throw new IllegalArgumentException("Coorte inconsistente");
        return fator("desenvolvimentoCompetencias",percentual(aprovados,conhecidos),razao(linhasValidas,candidatas),razao(conhecidos,pessoas),pessoas,pessoas,
            BigDecimal.valueOf(aprovados),BigDecimal.valueOf(conhecidos),"100 * A / K","training_records / employees",true,Map.of(),
            "Limiar 80 é adaptação proposta; não concede certificação nem XP");
    }
}
