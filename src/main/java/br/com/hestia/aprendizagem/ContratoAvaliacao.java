package br.com.hestia.aprendizagem;
import java.util.List;
import java.util.Map;

public final class ContratoAvaliacao {
    private ContratoAvaliacao(){}
    public record Questao(String id,String enunciado,Map<String,String> alternativas,String correta,String explicacao){}
    public record Publicacao(Long microtreinamentoId,int versao,List<Questao> questoes){}
    public record Resposta(String questaoId,String alternativa){}
    public record Envio(List<Resposta> respostas){}
    public record Liberacao(String chave,String justificativa,List<Long> leiturasReforco){}
    public record QuestaoPublica(String id,String enunciado,Map<String,String> alternativas){}
    public record Feedback(String questaoId,String enviada,String correta,String explicacao){}
    public record Tentativa(Long id,Long cicloId,int numero,Long usuarioId,Long empresaId,Long microtreinamentoId,
        int versao,String conteudoSha256,String status,Integer acertos,Integer nota,List<QuestaoPublica> questoes,List<Feedback> feedback){}
    public record Atualizada(Long usuarioId,Long empresaId){}
}
