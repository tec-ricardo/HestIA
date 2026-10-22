package br.com.hestia.indicadores.maturidade;
import br.com.hestia.aprendizagem.*;
import br.com.hestia.usuario.model.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.time.*;
import java.math.BigDecimal;
import java.util.*;
import static br.com.hestia.aprendizagem.AcessoCapacitacao.*;
@Service @Transactional
public class ApuracaoMaturidadeService {
    private final ResultadoMaturidadeService resultados;private final br.com.hestia.shared.RepositorioIntegracao db;
    private final ObjectProvider<ConfiancaIndicadoresPort> classificadores;
    private final ObjectMapper json;
    public ApuracaoMaturidadeService(ResultadoMaturidadeService resultados,br.com.hestia.shared.RepositorioIntegracao db,ObjectProvider<ConfiancaIndicadoresPort> classificadores){this.resultados=resultados;this.db=db;this.classificadores=classificadores;json=new ObjectMapper();var modulo=new SimpleModule();modulo.addSerializer(LocalDate.class,ToStringSerializer.instance);modulo.addSerializer(LocalDateTime.class,ToStringSerializer.instance);modulo.addSerializer(Instant.class,ToStringSerializer.instance);json.registerModule(modulo);}
    public record Consulta(ResultadoMaturidadeService.Resultado resultado,List<ConfiancaIndicadoresPort.Classificacao> confianca){
        public Map<String,BigDecimal> getNotasElegiveis(){return resultado.fatores().stream().filter(f->f.elegivelGlobal()&&f.nota()!=null).collect(java.util.stream.Collectors.toUnmodifiableMap(FatorCalculado::codigo,FatorCalculado::nota));}
    }
    public Consulta consultar(Usuario u,ResultadoMaturidadeService.Consulta c){var r=resultados.consultar(u,c);var porta=classificadores.getIfAvailable();var confianca=porta==null?r.fatores().stream().map(f->new ConfiancaIndicadoresPort.Classificacao(f.codigo(),"NAO_CLASSIFICADA","Classificador das SCRUM-427/428 ainda não integrado; consulte amostra, cobertura e elegibilidade","PENDENTE")).toList():porta.classificar(r);return new Consulta(r,confianca);}
    public Long registrar(Usuario u,ResultadoMaturidadeService.Consulta c){
        var resposta=consultar(u,c);var r=resposta.resultado();String preservado;try{preservado=json.writeValueAsString(resposta);}catch(Exception e){throw new IllegalStateException(e);}
        return db.inserir("INSERT INTO maturidade_apuracoes(empresa_id,departamento_id,autor_id,inicio,fim,fonte,snapshot,versao_pesos,versao_calculo,versao_parametros,conteudo,conteudo_sha256,registrada_em) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?)",r.empresaId(),r.departamentoId(),u.getId(),r.inicio(),r.fim(),r.naturezaDados().name(),r.hashSnapshot(),r.versaoPesos(),r.versaoCalculo(),r.versaoParametros(),preservado,hash(preservado),LocalDateTime.now());
    }
    void autorizar(Usuario u){db.acesso().empresa(u);exigir(u.getPerfil()!=PerfilUsuario.FUNCIONARIO,HttpStatus.FORBIDDEN,"Visão de apurações restrita a gestor e ADMIN");if(u.getPerfil()==PerfilUsuario.GESTOR)exigir(u.getDepartamento()!=null,HttpStatus.FORBIDDEN,"Departamento autorizado ausente");}
    public List<Map<String,Object>> historico(Usuario u){autorizar(u);String filtro=u.getPerfil()==PerfilUsuario.GESTOR?" AND departamento_id=?":"";var args=new ArrayList<Object>();args.add(db.acesso().empresa(u));if(!filtro.isEmpty())args.add(u.getDepartamento().getId());return db.jdbc().queryForList("SELECT id,departamento_id,inicio,fim,fonte,snapshot,versao_pesos,versao_calculo,versao_parametros,conteudo_sha256,registrada_em FROM maturidade_apuracoes WHERE empresa_id=?"+filtro+" ORDER BY id DESC LIMIT 100",args.toArray());}
    public Map<String,Object> obter(Usuario u,Long id){autorizar(u);var r=db.jdbc().queryForList("SELECT * FROM maturidade_apuracoes WHERE empresa_id=? AND id=?",db.acesso().empresa(u),id).stream().findFirst().orElseThrow(()->erro(HttpStatus.NOT_FOUND,"Apuração indisponível"));if(u.getPerfil()==PerfilUsuario.GESTOR)exigir(Objects.equals(r.get("departamento_id"),u.getDepartamento().getId()),HttpStatus.FORBIDDEN,"Escopo da apuração não autorizado");exigir(hash((String)r.get("conteudo")).equals(r.get("conteudo_sha256")),HttpStatus.CONFLICT,"Falha de integridade na apuração preservada");return r;}
    public Map<String,Object> evolucao(Usuario u,Long anterior,Long atual){
        var a=obter(u,anterior);var b=obter(u,atual);Map<String,Object> saida=new LinkedHashMap<>();saida.put("anteriorId",anterior);saida.put("atualId",atual);
        try{
            var ra=json.readTree((String)a.get("conteudo")).get("resultado");var rb=json.readTree((String)b.get("conteudo")).get("resultado");
            LocalDate ai=LocalDate.parse(ra.get("inicio").asText()),af=LocalDate.parse(ra.get("fim").asText()),bi=LocalDate.parse(rb.get("inicio").asText()),bf=LocalDate.parse(rb.get("fim").asText());
            boolean compativel=Objects.equals(a.get("departamento_id"),b.get("departamento_id"))&&bi.isAfter(af)&&java.time.temporal.ChronoUnit.DAYS.between(ai,af)==java.time.temporal.ChronoUnit.DAYS.between(bi,bf);
            for(String campo:List.of("versaoPesos","versaoCalculo","versaoParametros","parametros","naturezaDados","empresaOrigem","departamentoOrigem"))compativel&=Objects.equals(ra.get(campo),rb.get(campo));
            saida.put("comparavel",compativel);saida.put("motivo",compativel?"Escopo, versões, parâmetros e duração compatíveis":"Períodos sobrepostos, duração, escopo, natureza ou regras incompatíveis");
            BigDecimal delta=null;var ga=ra.get("scoreGlobal");var gb=rb.get("scoreGlobal");if(compativel&&!ga.isNull()&&!gb.isNull())delta=gb.decimalValue().subtract(ga.decimalValue());saida.put("variacaoGlobal",delta);
            var fatores=new LinkedHashMap<String,BigDecimal>();if(compativel)for(var fa:ra.get("fatores"))for(var fb:rb.get("fatores"))if(fa.get("codigo").equals(fb.get("codigo"))&&fa.get("elegivelGlobal").asBoolean()&&fb.get("elegivelGlobal").asBoolean()&&!fa.get("nota").isNull()&&!fb.get("nota").isNull())fatores.put(fa.get("codigo").asText(),fb.get("nota").decimalValue().subtract(fa.get("nota").decimalValue()));saida.put("variacaoFatoresElegiveis",fatores);return saida;
        }catch(com.fasterxml.jackson.core.JsonProcessingException e){throw new IllegalStateException(e);}
    }
}
