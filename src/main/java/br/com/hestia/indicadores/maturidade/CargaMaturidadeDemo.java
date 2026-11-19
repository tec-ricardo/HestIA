package br.com.hestia.indicadores.maturidade;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.Map;

/** Observações fictícias isoladas; não altera usuários, avaliações ou controles operacionais. */
@Component @Profile("demo")
@ConditionalOnProperty(name="hestia.demo.enabled",havingValue="true")
public class CargaMaturidadeDemo {
    public static final String VERSAO="demo-db-v1";
    private final JdbcTemplate jdbc;
    private final ObjectMapper json=new ObjectMapper();
    public CargaMaturidadeDemo(JdbcTemplate jdbc){this.jdbc=jdbc;}
    @EventListener(ApplicationReadyEvent.class) @Transactional
    public void carregar(){carregar(LocalDate.now().withDayOfMonth(1).minusMonths(1));}
    void carregar(LocalDate inicio){
        var departamentos=jdbc.queryForList("""
            SELECT d.id,d.empresa_id FROM departamentos d JOIN empresas e ON e.id=d.empresa_id
            WHERE e.cnpj='99999999000191' ORDER BY d.id
            """);
        for(var d:departamentos){
            long empresa=((Number)d.get("empresa_id")).longValue(),dep=((Number)d.get("id")).longValue();
            String prefixo=inicio+":"+dep+":";
            for(int i=1;i<=36;i++)inserir(empresa,dep,prefixo+"treino:"+i,"TREINAMENTO",inicio.plusDays(14),
                Map.of("pessoa","demo-"+dep+"-"+i,"nota",65+i%8*5));
            for(int i=1;i<=60;i++)inserir(empresa,dep,prefixo+"uso:"+i,"USO",inicio.plusDays(14),
                Map.of("autorizacao",i<=52?"APROVADA":"RETIRADA","conformidade",i<=50?"CONFORME":"NAO_CONFORME"));
            for(int i=1;i<=30;i++){
                inserir(empresa,dep,prefixo+"issue:"+i,"ISSUE",inicio.plusDays(14),Map.of("tipo","TAREFA","duracaoDias",1+i%10,"metaDias",5));
                inserir(empresa,dep,prefixo+"pr:"+i,"PR",inicio.plusDays(14),Map.of("numero",i,"revisaoIndependente",i<=20));
            }
            // Reproduz as premissas financeiras da SCRUM-570, neste exemplo fechado de demonstração.
            inserir(empresa,dep,prefixo+"financeiro","FINANCEIRO",inicio.withDayOfMonth(inicio.lengthOfMonth()),
                Map.of("brl",7164.56,"usd",17.53,"brlPorUsd",5,"orcamentoBrl",6357.74,"moedaOrcamento","BRL","mesCompleto",true));
            for(int i=1;i<=8;i++)inserir(empresa,dep,prefixo+"controle:"+i,"CONTROLE",inicio.plusDays(14),
                Map.of("controle","SEG0"+i,"resultado",i==5||i==7?"NAO_ATENDIDO":"ATENDIDO"));
        }
    }
    private void inserir(long empresa,long dep,String chave,String tipo,LocalDate data,Map<String,Object> dados){
        try{
            jdbc.update("""
                INSERT INTO maturidade_observacoes_demo(empresa_id,departamento_id,versao,chave,tipo,data_referencia,natureza,dados)
                VALUES(?,?,?,?,?,?,'SIMULADA',?) ON CONFLICT(empresa_id,versao,chave) DO NOTHING
                """,empresa,dep,VERSAO,chave,tipo,data,json.writeValueAsString(dados));
        }catch(com.fasterxml.jackson.core.JsonProcessingException e){throw new IllegalStateException("Não foi possível serializar a observação demo",e);}
    }
}
