package br.com.hestia.shared;
import br.com.hestia.aprendizagem.AcessoCapacitacao;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Component;
import java.sql.Statement;
import java.util.Objects;
@Component
public class RepositorioIntegracao {
    private final JdbcTemplate jdbc;private final AcessoCapacitacao acesso;private final ObjectMapper json=new ObjectMapper();
    public RepositorioIntegracao(JdbcTemplate jdbc,AcessoCapacitacao acesso){this.jdbc=jdbc;this.acesso=acesso;}
    public JdbcTemplate jdbc(){return jdbc;}public AcessoCapacitacao acesso(){return acesso;}public ObjectMapper json(){return json;}
    public String escrever(Object valor){try{return json.writeValueAsString(valor);}catch(Exception e){throw new IllegalStateException(e);}}
    public long inserir(String sql,Object... args){var chave=new GeneratedKeyHolder();jdbc.update(c->{var st=c.prepareStatement(sql,Statement.RETURN_GENERATED_KEYS);for(int i=0;i<args.length;i++)st.setObject(i+1,args[i]);return st;},chave);return Objects.requireNonNull(chave.getKeys()).entrySet().stream().filter(e->e.getKey().equalsIgnoreCase("id")).map(e->((Number)e.getValue()).longValue()).findFirst().orElseThrow();}
}
