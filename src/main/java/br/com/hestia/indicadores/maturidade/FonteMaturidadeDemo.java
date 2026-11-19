package br.com.hestia.indicadores.maturidade;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

/** Mesmas calculadoras dos indicadores; as observações demo são lidas do PostgreSQL. */
@Service @Profile("demo")
@ConditionalOnProperty(name="hestia.demo.enabled",havingValue="true")
public class FonteMaturidadeDemo {
    private final JdbcTemplate jdbc;private final CalculadorasMaturidade calculo;
    private final ObjectMapper json=new ObjectMapper();
    public FonteMaturidadeDemo(JdbcTemplate jdbc,CalculadorasMaturidade calculo){this.jdbc=jdbc;this.calculo=calculo;}
    record Observacao(long departamento,String tipo,LocalDate data,JsonNode dados){}
    public List<FatorCalculado> calcular(Long empresa,Long departamento,LocalDate inicio,LocalDate fim){
        if(inicio==null||fim==null||inicio.isAfter(fim))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Período inválido");
        if(java.time.temporal.ChronoUnit.MONTHS.between(inicio,fim)>24)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Consulte até 24 meses por vez na fonte DEMO");
        if(jdbc.queryForObject("SELECT COUNT(*) FROM empresas WHERE id=? AND cnpj='99999999000191'",Long.class,empresa)!=1)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Fonte DEMO restrita à empresa de demonstração");
        if(departamento!=null&&jdbc.queryForObject("SELECT COUNT(*) FROM departamentos WHERE id=? AND empresa_id=?",Long.class,departamento,empresa)!=1)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Departamento fora da empresa demo");
        List<Observacao> observacoes=jdbc.query("""
            SELECT o.departamento_id,o.tipo,o.data_referencia,o.dados
            FROM maturidade_observacoes_demo o JOIN departamentos d ON d.id=o.departamento_id AND d.empresa_id=o.empresa_id
            WHERE o.empresa_id=? AND (CAST(? AS BIGINT) IS NULL OR o.departamento_id=?)
              AND o.versao=? AND o.natureza='SIMULADA' AND o.data_referencia>=? AND o.data_referencia<=?
            ORDER BY o.id
            """,(rs,i)->{
                try{return new Observacao(rs.getLong(1),rs.getString(2),rs.getDate(3).toLocalDate(),json.readTree(rs.getString(4)));}
                catch(com.fasterxml.jackson.core.JsonProcessingException e){throw new IllegalStateException("Observação demo inválida",e);}
            },empresa,departamento,departamento,CargaMaturidadeDemo.VERSAO,inicio,fim);
        Map<String,List<BigDecimal>> notas=new HashMap<>();long candidatas=0;
        long usos=0,classificados=0,aprovados=0,conformes=0,naoConformes=0,revisoes=0;
        long prs=0,prsRevisados=0,controles=0,atendidos=0;
        List<BigDecimal> duracoes=new ArrayList<>();Set<BigDecimal> metas=new HashSet<>();
        BigDecimal custo=BigDecimal.ZERO,orcamento=BigDecimal.ZERO;
        boolean moedaValida=true,mesesCompletos=true;Set<String> centrosMeses=new HashSet<>(),controlesUnicos=new HashSet<>();
        for(var o:observacoes){var r=o.dados();switch(o.tipo()){
            case "TREINAMENTO"->{candidatas++;var nota=decimal(r,"nota");if(nota.signum()<0||nota.compareTo(new BigDecimal("100"))>0)throw invalido();
                notas.computeIfAbsent(texto(r,"pessoa"),k->new ArrayList<>()).add(nota);}
            case "USO"->{usos++;String autorizacao=texto(r,"autorizacao"),status=texto(r,"conformidade");
                if(!Set.of("APROVADA","RETIRADA","EM_ANALISE").contains(autorizacao))throw invalido();
                classificados++;if(autorizacao.equals("APROVADA"))aprovados++;
                switch(status){case "CONFORME"->conformes++;case "NAO_CONFORME"->naoConformes++;case "REQUER_REVISAO"->revisoes++;default->throw invalido();}}
            case "ISSUE"->{texto(r,"tipo");BigDecimal dias=decimal(r,"duracaoDias"),meta=decimal(r,"metaDias");
                if(dias.signum()<0||meta.signum()<=0)throw invalido();duracoes.add(dias);metas.add(meta.stripTrailingZeros());}
            case "PR"->{if(!r.path("numero").isIntegralNumber()||r.path("numero").asLong()<=0||!r.path("revisaoIndependente").isBoolean())throw invalido();
                prs++;if(r.path("revisaoIndependente").asBoolean())prsRevisados++;}
            case "FINANCEIRO"->{
                BigDecimal brl=decimal(r,"brl"),usd=decimal(r,"usd"),cambio=decimal(r,"brlPorUsd"),budget=decimal(r,"orcamentoBrl");
                if(brl.signum()<0||usd.signum()<0||cambio.signum()<=0||budget.signum()<=0)throw invalido();
                moedaValida&="BRL".equals(texto(r,"moedaOrcamento"));
                var mes=YearMonth.from(o.data());String chave=o.departamento()+":"+mes;
                if(!centrosMeses.add(chave))throw invalido();
                mesesCompletos&=r.path("mesCompleto").isBoolean()&&r.path("mesCompleto").asBoolean()
                    &&!inicio.isAfter(mes.atDay(1))&&!fim.isBefore(mes.atEndOfMonth());
                custo=custo.add(brl).add(usd.multiply(cambio));orcamento=orcamento.add(budget);}
            case "CONTROLE"->{String controle=texto(r,"controle"),status=texto(r,"resultado");
                if(!controle.matches("SEG0[1-8]")||!controlesUnicos.add(o.departamento()+":"+controle))throw invalido();
                if(!Set.of("ATENDIDO","NAO_ATENDIDO","NAO_VERIFICADO").contains(status))throw invalido();
                if(!status.equals("NAO_VERIFICADO"))controles++;if(status.equals("ATENDIDO"))atendidos++;}
            default->throw invalido();
        }}
        if(metas.size()>1)throw new ResponseStatusException(HttpStatus.CONFLICT,"As metas dos tipos de issue do cenário não são compatíveis");
        long pessoas=notas.size(),qualificadas=notas.values().stream().filter(xs->xs.get(xs.size()-1).compareTo(new BigDecimal("80"))>=0).count();
        long departamentos=jdbc.queryForObject("SELECT COUNT(*) FROM departamentos WHERE empresa_id=? AND (CAST(? AS BIGINT) IS NULL OR id=?)",Long.class,empresa,departamento,departamento);
        long mesesEsperados=0;for(var m=YearMonth.from(inicio);!m.isAfter(YearMonth.from(fim));m=m.plusMonths(1))
            if(!inicio.isAfter(m.atDay(1))&&!fim.isBefore(m.atEndOfMonth()))mesesEsperados++;
        mesesCompletos&=centrosMeses.size()==departamentos*mesesEsperados&&mesesEsperados>0;
        var fatores=List.of(
            calculo.literacy(notas,candidatas),
            calculo.governancaOperacional(usos,classificados,aprovados),
            calculo.usoResponsavel(conformes,naoConformes,revisoes,0,usos,conformes+naoConformes+revisoes),
            calculo.produtividade(duracoes,duracoes.size(),metas.stream().findFirst().orElse(null)),
            calculo.finOps(orcamento,custo,moedaValida,mesesCompletos,centrosMeses.size()),
            calculo.seguranca(atendidos,controles,8*departamentos),
            calculo.colaboracao(prsRevisados,prs,prs),
            calculo.competenciasOperacionais(qualificadas,pessoas,pessoas,candidatas,candidatas));
        return fatores.stream().map(f->{var avisos=new ArrayList<>(f.avisos());avisos.add("Dados fictícios persistidos no banco; não representam pessoas, faturamento ou controles reais. Oito controles por departamento.");
            return new FatorCalculado(f.codigo(),f.peso(),f.nota(),f.estado(),f.elegivelGlobal(),f.motivo(),f.coberturaTecnica(),f.coberturaClassificacao(),f.amostra(),f.populacao(),f.numerador(),f.denominador(),f.formula(),
                "PostgreSQL / maturidade_observacoes_demo / "+CargaMaturidadeDemo.VERSAO,f.estatisticas(),avisos,f.versaoCalculo()).comNatureza("DEMO");}).toList();
    }
    private static BigDecimal decimal(JsonNode r,String campo){if(!r.path(campo).isNumber())throw invalido();return r.get(campo).decimalValue();}
    private static String texto(JsonNode r,String campo){if(!r.path(campo).isTextual()||r.get(campo).asText().isBlank())throw invalido();return r.get(campo).asText();}
    private static ResponseStatusException invalido(){return new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,"Observações demo inválidas; nenhuma nota foi presumida");}
}
