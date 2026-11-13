package br.com.hestia.rag.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.util.*;
import static br.com.hestia.rag.service.ContextualizacaoRagService.Turno;

/** Seleção e resumo extrativo calculados somente sobre resultados revalidados. */
@Service
public class OrcamentoContextoRagService {
    public record Selecao(List<Turno> turnos,int caracteres,int completas,int truncadas,int resumidas,int omitidas,int limite,String estrategia){}
    private final int maximo,recentes,varredura,resumo;
    private final ObjectMapper json=new ObjectMapper();
    public OrcamentoContextoRagService(
        @Value("${hestia.rag.contexto.max-caracteres:6000}") int maximo,
        @Value("${hestia.rag.contexto.turnos-recentes:4}") int recentes,
        @Value("${hestia.rag.contexto.max-interacoes:100}") int varredura,
        @Value("${hestia.rag.contexto.resumo-caracteres:1200}") int resumo){
        if(maximo<512||maximo>32000||recentes<1||recentes>20||varredura<recentes||varredura>500||resumo<256||resumo>maximo/2)
            throw new IllegalArgumentException("Orçamento RAG inválido: confira limites de caracteres, resumo e interações");
        this.maximo=maximo;this.recentes=recentes;this.varredura=varredura;this.resumo=resumo;
    }
    public int maxInteracoes(){return varredura;}
    public int maxCaracteres(){return maximo;}
    public Selecao selecionar(List<InteracaoRagService.Resultado> revalidadas){
        List<InteracaoRagService.Resultado> elegiveis=new ArrayList<>();
        int inicio=Math.max(0,revalidadas.size()-varredura);
        for(var r:revalidadas.subList(inicio,revalidadas.size())){
            // Uma quebra de elegibilidade impede recuperar por resumo um assunto anterior.
            if(r.fontes().isEmpty()||r.resposta()==null){elegiveis.clear();continue;}
            elegiveis.add(r);
        }
        List<Turno> selecionados=new ArrayList<>();int primeira=elegiveis.size();
        int orcamentoRecente=maximo-resumo-2;
        for(int i=elegiveis.size()-1;i>=0&&selecionados.size()<recentes;i--){
            var turno=turno(elegiveis.get(i));var tentativa=new ArrayList<>(selecionados);tentativa.addFirst(turno);
            if(tamanho(tentativa)>orcamentoRecente){
                if(selecionados.isEmpty()){turno=encurtar(turno,orcamentoRecente-2);selecionados.add(turno);primeira=i;}
                break;
            }
            selecionados=tentativa;primeira=i;
        }
        int selecionadas=selecionados.size(),completas=0,truncadas=0,resumidas=0;
        for(int i=0;i<selecionados.size();i++){
            var original=elegiveis.get(primeira+i);var escolhido=selecionados.get(i);
            if(escolhido.pergunta().equals(original.pergunta())&&escolhido.resposta().equals(original.resposta()))completas++;else truncadas++;
        }
        List<String> extratos=new ArrayList<>();Set<Long> fontes=new LinkedHashSet<>();Turno resumoAtual=null;
        for(int i=primeira-1;i>=0;i--){
            var r=elegiveis.get(i);var ids=r.fontes().stream().map(InteracaoRagService.Fonte::id).toList();
            var novasFontes=new LinkedHashSet<>(fontes);novasFontes.addAll(ids);
            String extrato="Interação "+r.id()+"; fontes "+ids+"; pergunta: "+cortar(r.pergunta(),120)+"; resposta: "+cortar(r.resposta(),180);
            var novosExtratos=new ArrayList<>(extratos);novosExtratos.addFirst(extrato);
            var candidato=new Turno("Resumo extrativo parcial de mensagens anteriores; não é fonte normativa.",String.join("\n",novosExtratos),List.copyOf(novasFontes),true);
            if(tamanho(candidato)>resumo)break;
            fontes=novasFontes;extratos=novosExtratos;resumoAtual=candidato;resumidas++;
        }
        if(resumoAtual!=null)selecionados.addFirst(resumoAtual);
        if(tamanho(selecionados)>maximo)throw new IllegalStateException("Orçamento de histórico ultrapassado");
        return new Selecao(List.copyOf(selecionados),tamanho(selecionados),completas,truncadas,resumidas,elegiveis.size()-selecionadas-resumidas,maximo,"EXTRATIVO-1");
    }
    private Turno turno(InteracaoRagService.Resultado r){return new Turno(cortar(r.pergunta(),1000),cortar(r.resposta(),1500),r.fontes().stream().map(InteracaoRagService.Fonte::id).toList(),false);}
    private Turno encurtar(Turno t,int limite){
        while(tamanho(t)>limite){
            String p=t.pergunta(),r=t.resposta();
            if(r.length()>=p.length()&&r.length()>1)r=cortar(r,Math.max(1,r.length()/2));
            else if(p.length()>1)p=cortar(p,Math.max(1,p.length()/2));
            else throw new IllegalStateException("Metadados excedem o orçamento; aumente o limite");
            t=new Turno(p,r,t.fontes(),t.resumo());
        }
        return t;
    }
    int tamanho(Object valor){try{return json.writeValueAsString(valor).length();}catch(Exception e){throw new IllegalStateException(e);}}
    private String cortar(String s,int limite){if(s.length()<=limite)return s;int fim=limite;if(fim>0&&Character.isHighSurrogate(s.charAt(fim-1)))fim--;return s.substring(0,fim);}
}
