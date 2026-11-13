package br.com.hestia.indicadores.maturidade;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;

/** Seleção posterior à autorização de empresa/departamento e à validação histórica do catálogo. */
public final class SelecaoAiLiteracy {
    public static final String VERSAO="ai-literacy-selecao-1.0";
    public record Tentativa(long id,long usuarioId,long avaliacaoId,int versao,BigDecimal nota,
        String status,LocalDateTime finalizadaEm,boolean evidenciaElegivel) {}
    private record Chave(long usuario,long avaliacao,int versao) {}
    public record Selecao(List<Tentativa> tentativas,Map<String,List<BigDecimal>> notasPorPessoa,
        long avaliacoesCandidatas,long excluidas,String versao) {}
    private SelecaoAiLiteracy() {}
    public static Selecao selecionar(List<Tentativa> entradas,LocalDate inicio,LocalDate fim){
        Objects.requireNonNull(entradas);Objects.requireNonNull(inicio);Objects.requireNonNull(fim);
        if(inicio.isAfter(fim)||fim.equals(LocalDate.MAX))throw new IllegalArgumentException("Período inválido");
        var candidatas=new HashSet<Chave>();var ultimas=new HashMap<Chave,Tentativa>();var ids=new HashMap<Long,Tentativa>();
        var comparador=Comparator.comparing(Tentativa::finalizadaEm).thenComparingLong(Tentativa::id);
        for(var t:entradas){
            if(t==null||t.id()<=0||t.usuarioId()<=0||t.avaliacaoId()<=0||t.versao()<=0)throw new IllegalArgumentException("Identidade da tentativa inválida");
            var repetida=ids.putIfAbsent(t.id(),t);if(repetida!=null){if(!repetida.equals(t))throw new IllegalArgumentException("Tentativa duplicada conflitante");continue;}
            if(t.finalizadaEm()==null||t.finalizadaEm().isBefore(inicio.atStartOfDay())||!t.finalizadaEm().isBefore(fim.plusDays(1).atStartOfDay())
                ||!Set.of("APROVADA","REPROVADA").contains(t.status()==null?"":t.status())){continue;}
            var chave=new Chave(t.usuarioId(),t.avaliacaoId(),t.versao());candidatas.add(chave);
            if(!t.evidenciaElegivel()||t.nota()==null||t.nota().signum()<0||t.nota().compareTo(new BigDecimal("100"))>0){continue;}
            ultimas.merge(chave,t,(a,b)->comparador.compare(a,b)>=0?a:b);
        }
        var selecionadas=ultimas.values().stream().sorted(Comparator.comparingLong(Tentativa::usuarioId).thenComparingLong(Tentativa::avaliacaoId).thenComparingInt(Tentativa::versao)).toList();
        Map<String,List<BigDecimal>> notas=new TreeMap<>();for(var t:selecionadas)notas.computeIfAbsent(Long.toString(t.usuarioId()),x->new ArrayList<>()).add(t.nota());
        notas.replaceAll((k,v)->List.copyOf(v));
        return new Selecao(selecionadas,Map.copyOf(notas),candidatas.size(),ids.size()-selecionadas.size(),VERSAO);
    }
}
