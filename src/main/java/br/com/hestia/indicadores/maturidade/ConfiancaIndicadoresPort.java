package br.com.hestia.indicadores.maturidade;
import java.util.List;
/** Ponto de integração da classificação das SCRUM-427/428, sem duplicar sua regra. */
public interface ConfiancaIndicadoresPort {
    record Classificacao(String fator,String classificacao,String motivo,String versaoRegra){}
    List<Classificacao> classificar(ResultadoMaturidadeService.Resultado resultado);
}
