package br.com.hestia.indicadores.maturidade;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

class CalculadorasMaturidadeTest {
    private final CalculadorasMaturidade c=new CalculadorasMaturidade();
    private BigDecimal d(String s){return new BigDecimal(s);}
    @Test void literacyDaMesmoPesoACadaPessoa(){
        var r=c.literacy(Map.of("a",List.of(d("60"),d("60"),d("60")),"b",List.of(d("100"))),4);
        assertThat(r.nota()).isEqualByComparingTo("80");assertThat(r.amostra()).isEqualTo(2);assertThat(r.elegivelGlobal()).isFalse();
    }
    @Test void ausenciaDistingueZeroDeNulo(){
        assertThat(c.literacy(Map.of(),0).nota()).isNull();
        assertThat(c.literacy(Map.of("a",List.of(BigDecimal.ZERO)),1).nota()).isEqualByComparingTo("0");
        assertThat(c.colaboracao(0,0,0).nota()).isNull();
        assertThat(c.colaboracao(0,1,1).nota()).isEqualByComparingTo("0");
    }
    @Test void governancaPonderaRequestsMasNaoInventaHistorico(){
        var r=c.governanca(d("100"),d("100"),d("80"),30,true,false,BigDecimal.ONE);
        assertThat(r.nota()).isEqualByComparingTo("80");assertThat(r.estado()).isEqualTo("PARCIAL");
        assertThat(r.elegivelGlobal()).isFalse();
        assertThat(c.governanca(d("100"),d("50"),d("40"),30,false,false,BigDecimal.ONE).nota()).isNull();
    }
    @Test void usoResponsavelExcluiRevisaoDaNotaMasReduzCobertura(){
        var r=c.usoResponsavel(8,2,5,0,30,15);
        assertThat(r.nota()).isEqualByComparingTo("80");
        assertThat(r.coberturaClassificacao()).isBetween(d("0.6666"),d("0.6667"));
        assertThat(r.elegivelGlobal()).isFalse();assertThat(c.usoResponsavel(0,0,5,0,10,5).nota()).isNull();
        assertThat(c.usoResponsavel(8,2,5,5,30,15).coberturaTecnica()).isEqualByComparingTo("0.75");
    }
    @Test void produtividadeUsaMedianaEMetaExplicita(){
        assertThat(c.produtividade(List.of(d("0"),d("20")),2,d("5")).nota()).isEqualByComparingTo("50");
        var semMeta=c.produtividade(List.of(d("10")),1,null);
        assertThat(semMeta.estado()).isEqualTo("CONFIGURACAO_PENDENTE");assertThat(semMeta.nota()).isNull();
        assertThat(semMeta.estatisticas().get("medianaDias")).isEqualByComparingTo("10");
        assertThat(c.produtividade(List.of(BigDecimal.ZERO),1,d("5")).nota()).isEqualByComparingTo("100");
    }
    @Test void finOpsExigeMoedaObservacaoECompletude(){
        assertThat(c.finOps(d("1000"),d("1500"),true,true,1).nota()).isEqualByComparingTo("50");
        assertThat(c.finOps(d("1000"),d("2000"),true,true,1).nota()).isEqualByComparingTo("0");
        assertThat(c.finOps(d("1000"),BigDecimal.ZERO,true,true,1).nota()).isEqualByComparingTo("100");
        assertThat(c.finOps(d("1000"),d("1500"),false,true,1).estado()).isEqualTo("CONFIGURACAO_PENDENTE");
        assertThat(c.finOps(d("1000"),null,true,true,1).nota()).isNull();
        assertThat(c.finOps(BigDecimal.ZERO,d("100"),true,true,1).nota()).isNull();
        assertThat(c.finOps(d("1000"),d("100"),true,false,1).nota()).isNull();
    }
    @Test void segurancaExigeUniversoCompletoSemMinimoTrinta(){
        var parcial=c.seguranca(4,4,5);
        assertThat(parcial.nota()).isEqualByComparingTo("100");assertThat(parcial.coberturaTecnica()).isEqualByComparingTo("0.8");
        assertThat(parcial.elegivelGlobal()).isFalse();
        assertThat(c.seguranca(4,5,5).nota()).isEqualByComparingTo("80");assertThat(c.seguranca(4,5,5).elegivelGlobal()).isTrue();
        assertThat(c.seguranca(0,0,0).nota()).isNull();
    }
    @Test void colaboracaoECompetenciasPreservamDenominadores(){
        assertThat(c.colaboracao(6,10,10).nota()).isEqualByComparingTo("60");
        assertThat(c.competencias(7,10,10,10,10).nota()).isEqualByComparingTo("70");
        var parcial=c.competencias(7,8,10,10,10);
        assertThat(parcial.nota()).isEqualByComparingTo("87.5");assertThat(parcial.coberturaClassificacao()).isEqualByComparingTo("0.8");assertThat(parcial.elegivelGlobal()).isFalse();
    }
    @Test void criteriosDeAmostraECoberturaNaoSaoGarantiaEstatistica(){
        assertThat(c.colaboracao(30,30,30).elegivelGlobal()).isTrue();
        assertThat(c.colaboracao(29,29,29).elegivelGlobal()).isFalse();
        assertThat(c.colaboracao(30,30,40).elegivelGlobal()).isFalse();
    }
    @Test void rejeitaContagensMetasENotasInvalidas(){
        assertThatThrownBy(()->c.colaboracao(11,10,10)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(()->c.produtividade(List.of(d("-1")),1,d("5"))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(()->c.produtividade(List.of(d("1")),1,BigDecimal.ZERO)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(()->c.literacy(Map.of("a",List.of(d("101"))),1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(()->c.seguranca(6,5,5)).isInstanceOf(IllegalArgumentException.class);
    }
    private List<FatorCalculado> fatores(String nota,boolean semSeguranca){
        var pesos=Map.of("aiLiteracy","0.15","governanca","0.15","usoResponsavel","0.15","produtividade","0.10","finOps","0.10","seguranca","0.20","colaboracao","0.05","desenvolvimentoCompetencias","0.10");
        return pesos.entrySet().stream().map(p->new FatorCalculado(p.getKey(),d(p.getValue()),semSeguranca&&p.getKey().equals("seguranca")?null:d(nota),"CALCULADO",!(semSeguranca&&p.getKey().equals("seguranca")),"teste",BigDecimal.ONE,BigDecimal.ONE,30,30L,null,null,"teste","fixture",Map.of(),List.of(),CalculadorasMaturidade.VERSAO)).toList();
    }
    private ResultadoMaturidadeService.Resultado compor(List<FatorCalculado> f){
        return ResultadoMaturidadeService.compor(1L,null,"C01",null,new ResultadoMaturidadeService.Consulta(LocalDate.of(2026,1,1),LocalDate.of(2026,7,31),ResultadoMaturidadeService.Natureza.SINTETICA,"A".repeat(64),null,null,null),"A".repeat(64),"teste",Map.of(),f,"1.0");
    }
    @Test void composicaoPublicaGlobalApenasComOitoFatoresElegiveis(){
        var completo=compor(fatores("80",false));assertThat(completo.scoreGlobal()).isEqualByComparingTo("80");assertThat(completo.coberturaPesos()).isEqualByComparingTo("1");
        var parcial=compor(fatores("80",true));assertThat(parcial.scoreGlobal()).isNull();assertThat(parcial.scoreParcial()).isEqualByComparingTo("80");assertThat(parcial.coberturaPesos()).isEqualByComparingTo("0.8");assertThat(parcial.dimensoesAusentes()).containsExactly("seguranca");
    }
    @Test void arredondaSomenteResultadoEmHalfUp(){assertThat(compor(fatores("80.005",false)).scoreGlobal()).isEqualByComparingTo("80.01");}
}
