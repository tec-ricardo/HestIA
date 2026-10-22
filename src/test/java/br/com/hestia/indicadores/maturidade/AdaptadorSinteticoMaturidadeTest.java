package br.com.hestia.indicadores.maturidade;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Consumer;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.assertj.core.api.Assertions.*;

class AdaptadorSinteticoMaturidadeTest {
    private final SnapshotMaturidadeRepository origem=mock(SnapshotMaturidadeRepository.class);
    private final Map<String,List<Map<String,Object>>> tabelas=new HashMap<>();
    private final AdaptadorSinteticoMaturidade service=new AdaptadorSinteticoMaturidade(origem,new CalculadorasMaturidade());
    private final String hash="A".repeat(64);
    private final AdaptadorSinteticoMaturidade.Escopo escopo=new AdaptadorSinteticoMaturidade.Escopo("C01",null,hash,LocalDate.of(2026,1,1),LocalDate.of(2026,7,31),"Bug",new BigDecimal("5"));
    private Map<String,Object> row(Object... campos){Map<String,Object> r=new HashMap<>();for(int i=0;i<campos.length;i+=2)r.put((String)campos[i],campos[i+1]);return r;}
    @SuppressWarnings("unchecked") private void preparar(){
        when(origem.indice(eq("companies"),anyString(),eq(hash),eq("C01"))).thenReturn(Map.of("C01",row("company_id","C01")));
        when(origem.indice(eq("departments"),anyString(),eq(hash),eq("C01"))).thenReturn(Map.of("D01",row("department_name","Engenharia")));
        when(origem.indice(eq("employees"),anyString(),eq(hash),eq("C01"))).thenReturn(Map.of("E1",row("department_id","D01"),"E2",row("department_id","D01")));
        when(origem.indice(eq("projects"),anyString(),eq(hash),eq("C01"))).thenReturn(Map.of("P1",row("project_id","P1")));
        when(origem.indice(eq("ai_tools"),anyString(),eq(hash),eq("C01"))).thenReturn(Map.of("T1",row("ai_tool_id","T1")));
        doAnswer(call->{String tabela=call.getArgument(0);Consumer<Map<String,Object>> consumidor=call.getArgument(3);tabelas.getOrDefault(tabela,List.of()).forEach(consumidor);return null;}).when(origem).ler(anyString(),eq(hash),eq("C01"),any());
    }
    private FatorCalculado fator(String codigo){return service.calcular(escopo).stream().filter(f->f.codigo().equals(codigo)).findFirst().orElseThrow();}
    private Map<String,Object> treino(String chave,String pessoa,Object score,String status){return row("_chave_origem",chave,"training_id",chave,"employee_id",pessoa,"course_category","IA Generativa","enrollment_date","2026-01-02","completion_date","2026-02-01","completion_status",status,"score",score);}
    @Test void literacyAgrupaPessoasECompetenciasNaoApagamAprovacao(){
        preparar();tabelas.put("training_records",List.of(treino("a","E1",60,"Concluido"),treino("b","E1",60,"Concluido"),treino("c","E2",100,"Concluido"),treino("d","E2",null,"Concluido")));
        assertThat(fator("aiLiteracy").nota()).isEqualByComparingTo("80");assertThat(fator("aiLiteracy").coberturaTecnica()).isEqualByComparingTo("0.75");
        assertThat(fator("desenvolvimentoCompetencias").nota()).isEqualByComparingTo("50");
    }
    @Test void conclusaoSemScoreNaoViraReprovacaoNemZero(){
        preparar();tabelas.put("training_records",List.of(treino("a","E1",null,"Concluido"),treino("b","E2",100,"Concluido")));
        var f=fator("desenvolvimentoCompetencias");assertThat(f.nota()).isEqualByComparingTo("100");assertThat(f.coberturaClassificacao()).isEqualByComparingTo("0.5");assertThat(f.elegivelGlobal()).isFalse();
    }
    @Test void governancaNaoConfundeSucessoComAutorizacao(){
        preparar();tabelas.put("approved_ai_tools",List.of(row("ai_tool_id","T1","approval_status","Bloqueado","approval_date","2025-01-01","allowed_departments","Todos os departamentos")));
        tabelas.put("ai_usage_logs",List.of(row("usage_id","u1","_chave_origem","u1","employee_id","E1","department_id","D01","ai_tool_id","T1","timestamp","2026-02-01 12:00:00","request_count",80,"request_status","success")));
        assertThat(fator("governanca").nota()).isEqualByComparingTo("0");assertThat(fator("governanca").elegivelGlobal()).isFalse();
    }
    @Test void listaDeDepartamentosAmbiguaEVolumeInvalidoNaoSaoPermitidos(){
        preparar();tabelas.put("approved_ai_tools",List.of(row("ai_tool_id","T1","approval_status","Aprovado","approval_date","2025-01-01","allowed_departments","Nome inexistente")));
        var uso=row("usage_id","u1","_chave_origem","u1","employee_id","E1","department_id","D01","ai_tool_id","T1","timestamp","2026-02-01 12:00:00","request_count",80);
        tabelas.put("ai_usage_logs",List.of(uso));assertThat(fator("governanca").nota()).isNull();
        uso.put("request_count","1.5");assertThat(fator("governanca").estado()).isEqualTo("DADOS_INVALIDOS");
    }
    @Test void produtividadeSelecionaUmTipoECalculaMediana(){
        preparar();tabelas.put("issues",List.of(row("issue_id","i1","_chave_origem","i1","employee_id","E1","issue_type","Bug","created_at","2026-01-01","closed_at","2026-01-11"),row("issue_id","i2","_chave_origem","i2","employee_id","E2","issue_type","Task","created_at","2026-01-01","closed_at","2026-02-01")));
        var f=fator("produtividade");assertThat(f.nota()).isEqualByComparingTo("50");assertThat(f.amostra()).isEqualTo(1);
    }
    @Test void revisaoNulaReduzCoberturaEZeroEhConhecido(){
        preparar();tabelas.put("pull_requests",List.of(
            row("pr_id","p1","_chave_origem","p1","employee_id","E1","created_at","2026-01-01","merged_at","2026-01-02","review_count",1),
            row("pr_id","p2","_chave_origem","p2","employee_id","E2","created_at","2026-01-01","merged_at","2026-01-02","review_count",0),
            row("pr_id","p3","_chave_origem","p3","employee_id","E2","created_at","2026-01-01","merged_at","2026-01-02","review_count",null)));
        var f=fator("colaboracao");assertThat(f.nota()).isEqualByComparingTo("50");assertThat(f.coberturaTecnica()).isBetween(new BigDecimal("0.6666"),new BigDecimal("0.6667"));
    }
    @Test void idsConflitantesInvalidamFatorSemDuplicarObservacoes(){
        preparar();tabelas.put("training_records",List.of(treino("duplicado","E1",50,"Concluido"),treino("duplicado","E1",100,"Concluido")));
        assertThat(fator("aiLiteracy").estado()).isEqualTo("DADOS_INVALIDOS");
    }
    @Test void periodoVazioNaoInventaNotas(){
        preparar();var treino=treino("a","E1",100,"Concluido");treino.put("completion_date","2025-01-01");treino.put("enrollment_date","2025-01-01");tabelas.put("training_records",List.of(treino));
        assertThat(fator("aiLiteracy").nota()).isNull();assertThat(fator("desenvolvimentoCompetencias").nota()).isNull();
        assertThat(fator("finOps").estado()).isEqualTo("CONFIGURACAO_PENDENTE");assertThat(fator("usoResponsavel").estado()).isEqualTo("SEM_DADOS");
    }
}
