package br.com.hestia.rag.service;
import org.junit.jupiter.api.Test;
import java.util.*;
import java.time.LocalDateTime;
import static org.assertj.core.api.Assertions.*;
class OrcamentoContextoRagServiceTest {
    private InteracaoRagService.Resultado turno(long id,String pergunta,String resposta){return new InteracaoRagService.Resultado(id,pergunta,LocalDateTime.now(),"RECUPERADA",resposta,List.of(new InteracaoRagService.Fonte(id,"Política",resposta,1,"hash")),null,"NAO_CLASSIFICADA","CONSULTA","");}
    @Test void conversaCurtaPreservaTextoEFontes(){
        var service=new OrcamentoContextoRagService(6000,4,100,1200);
        var s=service.selecionar(List.of(turno(1,"Pergunta","Resposta"),turno(2,"Outra","Outra resposta")));
        assertThat(s.completas()).isEqualTo(2);assertThat(s.resumidas()).isZero();assertThat(s.omitidas()).isZero();
        assertThat(s.turnos().getFirst().pergunta()).isEqualTo("Pergunta");assertThat(s.turnos().getFirst().fontes()).containsExactly(1L);
    }
    @Test void conversaLongaTemResumoExtrativoParcialComRastreio(){
        var service=new OrcamentoContextoRagService(6000,4,100,1200);
        var entradas=new ArrayList<InteracaoRagService.Resultado>();for(long i=1;i<=40;i++)entradas.add(turno(i,"Pergunta "+i,"Trecho autorizado "+i));
        var s=service.selecionar(entradas);
        assertThat(s.caracteres()).isLessThanOrEqualTo(6000);assertThat(s.resumidas()).isPositive();assertThat(s.omitidas()).isPositive();
        assertThat(s.turnos().getFirst().resumo()).isTrue();assertThat(s.turnos().getFirst().resposta()).contains("Trecho autorizado");
        assertThat(s.turnos().getLast().pergunta()).isEqualTo("Pergunta 40");assertThat(s.completas()+s.resumidas()+s.omitidas()).isEqualTo(40);
    }
    @Test void orcamentoContaEscapesJsonETextosGrandes(){
        var service=new OrcamentoContextoRagService(1200,4,100,400);
        var s=service.selecionar(List.of(turno(1,"\"\n".repeat(2000),"🙂\"\n".repeat(3000))));
        assertThat(s.caracteres()).isEqualTo(service.tamanho(s.turnos())).isLessThanOrEqualTo(1200);
        assertThat(s.completas()).isZero();assertThat(s.truncadas()).isEqualTo(1);
    }
    @Test void retiradaNoFimEliminaTodaAMemoriaAnterior(){
        var service=new OrcamentoContextoRagService(6000,4,100,1200);
        var removido=new InteracaoRagService.Resultado(2L,"Privada",LocalDateTime.now(),"FONTES_INDISPONIVEIS",null,List.of(),null,"NAO_CLASSIFICADA","PLATAFORMA","");
        assertThat(service.selecionar(List.of(turno(1,"Assunto antigo","Dado retirado"),removido)).turnos()).isEmpty();
        var s=service.selecionar(List.of(turno(1,"Assunto antigo","Dado retirado"),removido,turno(3,"Novo assunto","Texto vigente")));
        assertThat(s.turnos()).hasSize(1);assertThat(s.turnos().getFirst().resposta()).isEqualTo("Texto vigente");
    }
    @Test void limiteDeVarreduraNaoReintroduzConversasForaDaJanela(){
        var service=new OrcamentoContextoRagService(6000,2,3,1200);
        var s=service.selecionar(List.of(turno(1,"Descartar","Não deve aparecer"),turno(2,"Antiga","Texto"),turno(3,"Nova","Texto"),turno(4,"Última","Texto")));
        assertThat(s.completas()+s.resumidas()+s.omitidas()).isEqualTo(3);assertThat(s.turnos().toString()).doesNotContain("Não deve aparecer");
    }
    @Test void configuracoesInconsistentesFalhamNaInicializacao(){
        assertThatThrownBy(()->new OrcamentoContextoRagService(100,4,100,1200)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(()->new OrcamentoContextoRagService(6000,4,2,1200)).isInstanceOf(IllegalArgumentException.class);
    }
}
