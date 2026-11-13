package br.com.hestia.indicadores.maturidade;
import java.math.*;import java.util.*;import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
class ClassificacaoRiscoDepartamentalTest {
 final CalculadorasMaturidade c=new CalculadorasMaturidade();final ClassificacaoRiscoDepartamental regra=new ClassificacaoRiscoDepartamental();
 List<FatorCalculado> fatores(String nota){BigDecimal n=new BigDecimal(nota);return List.of(c.governanca(new BigDecimal("100"),new BigDecimal("100"),n,30,true,true,BigDecimal.ONE),c.usoResponsavel(n.intValue(),100-n.intValue(),0,0,100,100),c.seguranca(n.intValue()*8/100,8,8));}
 @Test void limitesSaoDecididosAntesDeArredondar(){assertThat(regra.classificar(fatores("76")).classificacao()).isEqualTo(ClassificacaoRiscoDepartamental.Faixa.BAIXO);assertThat(regra.classificar(fatores("75")).classificacao()).isEqualTo(ClassificacaoRiscoDepartamental.Faixa.MEDIO);assertThat(regra.classificar(fatores("50")).classificacao()).isEqualTo(ClassificacaoRiscoDepartamental.Faixa.ALTO);assertThat(regra.classificar(fatores("100")).indice()).isEqualByComparingTo("0");}
 @Test void ausenciaNaoViraZeroNemRedistribuiPesos(){var f=new ArrayList<>(fatores("90"));f.removeLast();var r=regra.classificar(f);assertThat(r.indice()).isNull();assertThat(r.classificacao()).isNull();assertThat(r.estado()).isEqualTo("SEM_CLASSIFICACAO");assertThat(r.pendencias()).isNotEmpty();}
 @Test void amostraPequenaMostraFaixaApenasIndicativa(){var f=new ArrayList<>(fatores("100"));f.set(0,c.governanca(BigDecimal.TEN,BigDecimal.TEN,BigDecimal.TEN,10,true,true,BigDecimal.ONE));var r=regra.classificar(f);assertThat(r.fragil()).isTrue();assertThat(r.classificacao()).isNull();assertThat(r.classificacaoIndicativa()).isEqualTo(ClassificacaoRiscoDepartamental.Faixa.BAIXO);assertThat(r.evidencias()).hasSize(3);}
 @Test void naoMisturaSinteticoComOperacionalNemAceitaDuplicatas(){var f=new ArrayList<>(fatores("90"));f.set(0,f.get(0).comNatureza("SINTETICA"));f.set(1,f.get(1).comNatureza("OPERACIONAL"));assertThat(regra.classificar(f).indice()).isNull();assertThatThrownBy(()->regra.classificar(List.of(f.get(0),f.get(0)))).isInstanceOf(IllegalArgumentException.class);}

 @Test void arredondamentoNaoMudaFaixaEExportaCenariosReproduziveis() throws Exception {
  var f=new ArrayList<>(fatores("100"));f.set(0,c.governanca(new BigDecimal("100"),new BigDecimal("100"),new BigDecimal("16.666667"),30,true,true,BigDecimal.ONE));
  var limite=regra.classificar(f);assertThat(limite.indice()).isEqualByComparingTo("25.00");assertThat(limite.classificacao()).isEqualTo(ClassificacaoRiscoDepartamental.Faixa.BAIXO);
  String destino=System.getProperty("hestia.evidencias.risco");
  if(destino!=null)new com.fasterxml.jackson.databind.ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(new java.io.File(destino),Map.of("natureza","CENARIOS_DIDATICOS_DE_TESTE","versao",ClassificacaoRiscoDepartamental.VERSAO,"cenarios",List.of(regra.classificar(fatores("100")),regra.classificar(fatores("75")),regra.classificar(fatores("50")),limite,regra.classificar(List.of()))));
 }
}
