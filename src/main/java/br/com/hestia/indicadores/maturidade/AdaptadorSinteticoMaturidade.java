package br.com.hestia.indicadores.maturidade;

import org.springframework.stereotype.Service;
import java.util.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.function.Supplier;
import static br.com.hestia.indicadores.maturidade.SnapshotMaturidadeRepository.texto;

@Service
public class AdaptadorSinteticoMaturidade {
    private final SnapshotMaturidadeRepository origem;
    private final CalculadorasMaturidade calculo;
    public AdaptadorSinteticoMaturidade(SnapshotMaturidadeRepository origem,CalculadorasMaturidade calculo){this.origem=origem;this.calculo=calculo;}
    public record Escopo(String empresa,String departamento,String hash,LocalDate inicio,LocalDate fim,String tipoIssue,BigDecimal metaDias){}
    public List<FatorCalculado> calcular(Escopo e) {
        var departamentos=origem.indice("departments","department_id",e.hash(),e.empresa());
        var empregados=origem.indice("employees","employee_id",e.hash(),e.empresa());
        if(!origem.indice("companies","company_id",e.hash(),e.empresa()).containsKey(e.empresa()))
            throw new IllegalStateException("Snapshot sem empresa de origem autorizada");
        if(e.departamento()!=null&&!departamentos.containsKey(e.departamento()))throw new IllegalArgumentException("Departamento não pertence à empresa/snapshot");
        return List.of(
            seguro("aiLiteracy",()->literacy(e,empregados,departamentos)),
            seguro("governanca",()->governanca(e,empregados,departamentos)),
            calculo.pendente("usoResponsavel","SEM_DADOS","A fonte sintética não possui avaliações operacionais com fundamentos","avaliacoes_conformidade"),
            seguro("produtividade",()->produtividade(e,empregados,departamentos)),
            calculo.pendente("finOps","CONFIGURACAO_PENDENTE","Moeda dos orçamentos e política de conversão/completude não configuradas","ai_billing / ai_budgets"),
            calculo.pendente("seguranca","CONFIGURACAO_PENDENTE","Catálogo de controles aplicáveis e evidências autorizadas ausentes","Controles e evidências"),
            seguro("colaboracao",()->colaboracao(e,empregados,departamentos)),
            seguro("desenvolvimentoCompetencias",()->competencias(e,empregados,departamentos)));
    }
    private FatorCalculado seguro(String fator,Supplier<FatorCalculado> tarefa){
        try{return tarefa.get();}catch(IllegalStateException ex){return calculo.pendente(fator,"DADOS_INVALIDOS",ex.getMessage(),"base_sintetica.import_linhas");}
    }
    static BigDecimal decimal(Map<String,Object> r,String campo){
        try {var s=texto(r,campo);return s==null?null:new BigDecimal(s);}catch(NumberFormatException ex){return null;}
    }
    static Long inteiro(Map<String,Object> r,String campo){
        try{var v=decimal(r,campo);return v==null?null:v.longValueExact();}catch(ArithmeticException ex){return null;}
    }
    static LocalDateTime data(Map<String,Object> r,String campo){
        try {var s=texto(r,campo);if(s==null)return null;
            return s.length()==10?LocalDate.parse(s).atStartOfDay():LocalDateTime.parse(s.replace(' ','T'));
        }catch(java.time.format.DateTimeParseException ex){return null;}
    }
    private boolean periodo(LocalDateTime d,Escopo e){return d!=null&&!d.isBefore(e.inicio().atStartOfDay())&&d.isBefore(e.fim().plusDays(1).atStartOfDay());}
    private boolean vinculo(String pessoa,Map<String,Map<String,Object>> empregados,Map<String,Map<String,Object>> departamentos){
        var p=empregados.get(pessoa);return p!=null&&departamentos.containsKey(texto(p,"department_id"));
    }
    private boolean recorte(String pessoa,Escopo e,Map<String,Map<String,Object>> empregados,Map<String,Map<String,Object>> departamentos){
        if(e.departamento()==null)return true;
        var p=empregados.get(pessoa);
        // Autoria desconhecida reduz a cobertura do recorte, sem ser atribuída a outro departamento.
        return p==null||texto(p,"department_id")==null||!departamentos.containsKey(texto(p,"department_id"))||e.departamento().equals(texto(p,"department_id"));
    }
    private void distinta(Map<String,String> ids,Map<String,Object> r,String campo){
        String id=texto(r,campo);if(id==null)id=texto(r,"_chave_origem");
        String anterior=ids.putIfAbsent(id,texto(r,"_chave_origem"));
        if(anterior!=null)throw new IllegalStateException("ID de origem repetido/conflitante: "+campo);
    }
    private FatorCalculado literacy(Escopo e,Map<String,Map<String,Object>> empregados,Map<String,Map<String,Object>> departamentos){
        Map<String,List<BigDecimal>> scores=new HashMap<>();long[] candidatos={0};Map<String,String> ids=new HashMap<>();
        origem.ler("training_records",e.hash(),e.empresa(),r->{
            if(!"IA Generativa".equals(texto(r,"course_category"))||!"Concluido".equals(texto(r,"completion_status")))return;
            String pessoa=texto(r,"employee_id");if(!recorte(pessoa,e,empregados,departamentos))return;
            LocalDateTime fim=data(r,"completion_date");if(fim!=null&&!periodo(fim,e))return;
            candidatos[0]++;distinta(ids,r,"training_id");
            var nota=decimal(r,"score");var inicio=data(r,"enrollment_date");
            if(!vinculo(pessoa,empregados,departamentos)||fim==null||inicio==null||fim.isBefore(inicio)
                    ||nota==null||nota.signum()<0||nota.compareTo(new BigDecimal("100"))>0)return;
            scores.computeIfAbsent(pessoa,k->new ArrayList<>()).add(nota);
        });
        return calculo.literacy(scores,candidatos[0]);
    }
    private FatorCalculado competencias(Escopo e,Map<String,Map<String,Object>> empregados,Map<String,Map<String,Object>> departamentos){
        // 0=conhecida sem aprovação, 1=indeterminada, 2=aprendizagem demonstrada.
        Map<String,Integer> pessoas=new HashMap<>();long[] linhas={0,0};Map<String,String> ids=new HashMap<>();
        origem.ler("training_records",e.hash(),e.empresa(),r->{
            String pessoa=texto(r,"employee_id");if(!recorte(pessoa,e,empregados,departamentos))return;
            var inicio=data(r,"enrollment_date");if(inicio!=null&&!periodo(inicio,e))return;
            linhas[0]++;distinta(ids,r,"training_id");
            if(inicio==null||!vinculo(pessoa,empregados,departamentos))return;
            linhas[1]++;int estado=1;String status=texto(r,"completion_status");var fim=data(r,"completion_date");var nota=decimal(r,"score");
            if(Set.of("Em Andamento","Nao Iniciado","Abandonado").contains(status==null?"":status))estado=0;
            else if("Concluido".equals(status)&&fim!=null&&!fim.isBefore(inicio)){
                if(!fim.isBefore(e.fim().plusDays(1).atStartOfDay()))estado=0;
                else if(nota!=null&&nota.signum()>=0&&nota.compareTo(new BigDecimal("100"))<=0)estado=nota.compareTo(new BigDecimal("80"))>=0?2:0;
            }
            pessoas.merge(pessoa,estado,Math::max);
        });
        long aprovados=pessoas.values().stream().filter(x->x==2).count();long conhecidos=pessoas.values().stream().filter(x->x!=1).count();
        return calculo.competencias(aprovados,conhecidos,pessoas.size(),linhas[1],linhas[0]);
    }
    private FatorCalculado colaboracao(Escopo e,Map<String,Map<String,Object>> empregados,Map<String,Map<String,Object>> departamentos){
        origem.indice("projects","project_id",e.hash(),e.empresa());
        long[] n={0,0,0};Map<String,String> ids=new HashMap<>();
        origem.ler("pull_requests",e.hash(),e.empresa(),r->{
            String pessoa=texto(r,"employee_id");if(!recorte(pessoa,e,empregados,departamentos))return;
            var fim=data(r,"merged_at");
            if(fim==null){if(texto(r,"merged_at")==null)return;}else if(!periodo(fim,e))return;
            n[0]++;distinta(ids,r,"pr_id");var inicio=data(r,"created_at");var revisoes=inteiro(r,"review_count");
            if(fim==null||inicio==null||fim.isBefore(inicio)||!vinculo(pessoa,empregados,departamentos)||revisoes==null||revisoes<0)return;
            n[1]++;if(revisoes>=1)n[2]++;
        });
        return calculo.colaboracao(n[2],n[1],n[0]);
    }
    private FatorCalculado produtividade(Escopo e,Map<String,Map<String,Object>> empregados,Map<String,Map<String,Object>> departamentos){
        if(e.tipoIssue()==null)return calculo.pendente("produtividade","CONFIGURACAO_PENDENTE","Selecione um único tipo de issue e configure sua meta versionada","issues / projects");
        origem.indice("projects","project_id",e.hash(),e.empresa());
        long[] candidatos={0};List<BigDecimal> duracoes=new ArrayList<>();Map<String,String> ids=new HashMap<>();
        origem.ler("issues",e.hash(),e.empresa(),r->{
            if(!e.tipoIssue().equals(texto(r,"issue_type")))return;
            String pessoa=texto(r,"employee_id");if(!recorte(pessoa,e,empregados,departamentos))return;
            var fim=data(r,"closed_at");if(fim==null){if(texto(r,"closed_at")==null)return;}else if(!periodo(fim,e))return;
            candidatos[0]++;distinta(ids,r,"issue_id");var inicio=data(r,"created_at");
            if(fim==null||inicio==null||fim.isBefore(inicio)||!vinculo(pessoa,empregados,departamentos))return;
            var duracao=Duration.between(inicio,fim);
            duracoes.add(BigDecimal.valueOf(duracao.getSeconds()).add(BigDecimal.valueOf(duracao.getNano(),9)).divide(new BigDecimal("86400"),java.math.MathContext.DECIMAL128));
        });
        return calculo.produtividade(duracoes,candidatos[0],e.metaDias());
    }
    private FatorCalculado governanca(Escopo e,Map<String,Map<String,Object>> empregados,Map<String,Map<String,Object>> departamentos){
        var ferramentas=origem.indice("ai_tools","ai_tool_id",e.hash(),e.empresa());
        Map<String,List<Map<String,Object>>> aprovacoes=new HashMap<>();
        origem.ler("approved_ai_tools",e.hash(),e.empresa(),r->{String ferramenta=texto(r,"ai_tool_id");if(ferramenta!=null)aprovacoes.computeIfAbsent(ferramenta,k->new ArrayList<>()).add(r);});
        BigDecimal[] volumes={BigDecimal.ZERO,BigDecimal.ZERO,BigDecimal.ZERO};long[] linhas={0,0};boolean[] volumeConhecido={true};Map<String,String> ids=new HashMap<>();
        origem.ler("ai_usage_logs",e.hash(),e.empresa(),r->{
            String pessoa=texto(r,"employee_id");if(!recorte(pessoa,e,empregados,departamentos))return;
            var instante=data(r,"timestamp");if(instante!=null&&!periodo(instante,e))return;
            linhas[0]++;distinta(ids,r,"usage_id");var requests=inteiro(r,"request_count");
            if(requests==null||requests<=0){volumeConhecido[0]=false;return;}
            volumes[0]=volumes[0].add(BigDecimal.valueOf(requests));
            String dep=texto(r,"department_id"),ferramenta=texto(r,"ai_tool_id");
            if(instante==null||dep==null||ferramenta==null||!ferramentas.containsKey(ferramenta)||!vinculo(pessoa,empregados,departamentos)
                    ||!dep.equals(texto(empregados.get(pessoa),"department_id"))||!departamentos.containsKey(dep))return;
            linhas[1]++;var cadastros=aprovacoes.get(ferramenta);if(cadastros==null||cadastros.size()!=1)return;
            var cadastro=cadastros.getFirst();String status=texto(cadastro,"approval_status");var dataAprovacao=data(cadastro,"approval_date");
            if(dataAprovacao==null||dataAprovacao.isAfter(instante))return;
            String permitidos=texto(cadastro,"allowed_departments");if(permitidos==null)return;
            boolean permite=false;
            if("Todos os departamentos".equals(permitidos))permite=true;
            else {
                Set<String> resolvidos=new HashSet<>();
                for(String nome:permitidos.split("[,;]",-1)){
                    final String canonico=nome.trim();
                    var matches=departamentos.entrySet().stream().filter(d->canonico.equals(texto(d.getValue(),"department_name"))).map(Map.Entry::getKey).toList();
                    if(matches.size()!=1)return;resolvidos.add(matches.getFirst());
                }
                permite=resolvidos.contains(dep);
            }
            if(!Set.of("Aprovado","Bloqueado","Em Avaliacao").contains(status==null?"":status))return;
            volumes[1]=volumes[1].add(BigDecimal.valueOf(requests));
            if("Aprovado".equals(status)&&permite)volumes[2]=volumes[2].add(BigDecimal.valueOf(requests));
        });
        return calculo.governanca(volumes[0],volumes[1],volumes[2],linhas[0],volumeConhecido[0],false,CalculadorasMaturidade.razao(linhas[1],linhas[0]));
    }
}
