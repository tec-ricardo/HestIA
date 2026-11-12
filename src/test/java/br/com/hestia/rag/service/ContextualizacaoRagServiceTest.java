package br.com.hestia.rag.service;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import static org.assertj.core.api.Assertions.*;
class ContextualizacaoRagServiceTest {
    private final ContextualizacaoRagService service=new ContextualizacaoRagService("");
    @Test void perguntaIndependenteNaoPrecisaDeModelo(){assertThat(service.interpretar("Posso enviar dados pessoais para IA?",List.of()).consulta()).isEqualTo("Posso enviar dados pessoais para IA?");}
    @Test void referenciaSemHistoricoPedeEsclarecimento(){for(String pergunta:List.of("E nesse caso?","Posso enviar isso?","Ela está autorizada?","Por quê?")){var r=service.interpretar(pergunta,List.of());assertThat(r.consulta()).isNull();assertThat(r.esclarecimento()).isNotBlank();}}
    @Test void interpretaConsultaOuEsclarecimentoSemAceitarAmbos(){
        assertThat(service.interpretarJson("{\"consulta\":\"Posso enviar dados anonimizados?\",\"esclarecimento\":null}").consulta()).contains("anonimizados");
        assertThat(service.interpretarJson("{\"consulta\":null,\"esclarecimento\":\"Qual ferramenta?\"}").esclarecimento()).isEqualTo("Qual ferramenta?");
        for(String texto:List.of("Resposta sem JSON","{}","{\"consulta\":\"x\",\"esclarecimento\":\"y\"}"))assertThatThrownBy(()->service.interpretarJson(texto)).isInstanceOf(ResponseStatusException.class);
    }
}
