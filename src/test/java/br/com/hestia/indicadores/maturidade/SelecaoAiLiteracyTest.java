package br.com.hestia.indicadores.maturidade;
import java.math.*;import java.time.*;import java.util.*;import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
class SelecaoAiLiteracyTest {
 final LocalDate inicio=LocalDate.of(2026,11,3),fim=LocalDate.of(2026,11,12);
 SelecaoAiLiteracy.Tentativa t(long id,long pessoa,long avaliacao,int versao,int nota,String data){return new SelecaoAiLiteracy.Tentativa(id,pessoa,avaliacao,versao,BigDecimal.valueOf(nota),"APROVADA",LocalDateTime.parse(data),true);}
 @Test void ultimaValidaPorVersaoIncluiReprovacaoEdesempataPorId(){var a=t(1,1,10,1,90,"2026-11-04T10:00:00");var b=new SelecaoAiLiteracy.Tentativa(2,1,10,1,new BigDecimal("40"),"REPROVADA",a.finalizadaEm(),true);var c=t(3,1,10,2,80,"2026-11-05T10:00:00");var r=SelecaoAiLiteracy.selecionar(List.of(c,a,b,a),inicio,fim);assertThat(r.tentativas()).extracting(SelecaoAiLiteracy.Tentativa::id).containsExactly(2L,3L);assertThat(r.avaliacoesCandidatas()).isEqualTo(2);}
 @Test void periodoInclusivoEnotaMediaDaPessoaTemMesmoPeso(){var r=SelecaoAiLiteracy.selecionar(List.of(t(1,1,10,1,20,"2026-11-03T00:00:00"),t(2,1,11,1,100,"2026-11-12T23:59:59"),t(3,2,10,1,100,"2026-11-05T00:00:00"),t(4,2,11,1,0,"2026-11-13T00:00:00")),inicio,fim);assertThat(new CalculadorasMaturidade().literacy(r.notasPorPessoa(),r.avaliacoesCandidatas()).nota()).isEqualByComparingTo("80");}
 @Test void tentativaInvalidaMaisRecenteNaoApagaValidaAnterior(){var a=t(1,1,10,1,80,"2026-11-04T10:00:00");var b=new SelecaoAiLiteracy.Tentativa(2,1,10,1,BigDecimal.TEN,"APROVADA",a.finalizadaEm().plusDays(1),false);assertThat(SelecaoAiLiteracy.selecionar(List.of(a,b),inicio,fim).tentativas()).containsExactly(a);}
 @Test void duplicataConflitanteEperiodoInvalidoSaoRecusados(){var a=t(1,1,10,1,80,"2026-11-04T10:00:00");assertThatThrownBy(()->SelecaoAiLiteracy.selecionar(List.of(a,t(1,1,10,1,90,"2026-11-04T10:00:00")),inicio,fim)).isInstanceOf(IllegalArgumentException.class);assertThatThrownBy(()->SelecaoAiLiteracy.selecionar(List.of(),fim,inicio)).isInstanceOf(IllegalArgumentException.class);}
}
