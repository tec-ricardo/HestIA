package br.com.hestia.indicadores.maturidade;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.util.*;
import java.util.function.Consumer;

/** Leitura apenas do snapshot e da empresa autorizados. Nunca importa dados ou cria usuários. */
@Repository
public class SnapshotMaturidadeRepository {
    private static final Set<String> DIRETAS=Set.of("companies","departments","employees","projects",
            "training_records","ai_usage_logs","approved_ai_tools","ai_billing","ai_budgets","security_events");
    private static final Set<String> VIA_PROJETO=Set.of("issues","pull_requests");
    private final JdbcTemplate jdbc;
    private final ObjectMapper json=new ObjectMapper();
    public SnapshotMaturidadeRepository(JdbcTemplate jdbc){this.jdbc=jdbc;}

    public void ler(String tabela,String hash,String empresa,Consumer<Map<String,Object>> consumidor) {
        boolean projeto=VIA_PROJETO.contains(tabela);
        if(!projeto&&!DIRETAS.contains(tabela)&&!tabela.equals("ai_tools"))throw new IllegalArgumentException("Tabela não permitida");
        String filtro=tabela.equals("ai_tools")?"""
            EXISTS (SELECT 1 FROM base_sintetica.import_linhas uso
                WHERE uso.origem_sha256=l.origem_sha256 AND uso.tabela='ai_usage_logs'
                  AND uso.dados->>'company_id'=? AND uso.dados->>'ai_tool_id'=l.dados->>'ai_tool_id')
            """:projeto?"""
            EXISTS (SELECT 1 FROM base_sintetica.import_linhas p
                WHERE p.origem_sha256 = l.origem_sha256 AND p.tabela = 'projects'
                  AND p.dados->>'company_id' = ?
                  AND p.dados->>'project_id' = l.dados->>'project_id')
            """:"l.dados->>'company_id' = ?";
        String sql="SELECT l.chave_origem, CAST(l.dados AS text) AS dados FROM base_sintetica.import_linhas l "
                +"WHERE l.origem_sha256 = ? AND l.tabela = ? AND "+filtro+" ORDER BY l.chave_origem";
        Set<String> chaves=new HashSet<>();
        jdbc.query(connection->{
            var st=connection.prepareStatement(sql);st.setFetchSize(1000);
            st.setString(1,hash);st.setString(2,tabela);st.setString(3,empresa);return st;
        },rs->{
            String chave=rs.getString("chave_origem");
            if(!chaves.add(chave))throw new IllegalStateException("Chave de origem duplicada no snapshot: "+tabela);
            try {
                Map<String,Object> dados=json.readValue(rs.getString("dados"),new TypeReference<Map<String,Object>>(){});
                dados.put("_chave_origem",chave);consumidor.accept(dados);
            }catch(com.fasterxml.jackson.core.JsonProcessingException e){throw new IllegalStateException("JSON de origem inválido",e);}
        });
    }

    public Map<String,Map<String,Object>> indice(String tabela,String campo,String hash,String empresa) {
        if(tabela.equals("projects")) {
            Long conflitos=jdbc.queryForObject("""
                SELECT count(*) FROM base_sintetica.import_linhas p
                WHERE p.origem_sha256=? AND p.tabela='projects' AND p.dados->>'company_id'=?
                AND (SELECT count(*) FROM base_sintetica.import_linhas outro
                     WHERE outro.origem_sha256=p.origem_sha256 AND outro.tabela='projects'
                       AND outro.dados->>'project_id'=p.dados->>'project_id') > 1
                """,Long.class,hash,empresa);
            if(conflitos!=null&&conflitos>0)throw new IllegalStateException("Projeto de origem ambíguo no snapshot");
        }
        Map<String,Map<String,Object>> indice=new HashMap<>();
        ler(tabela,hash,empresa,r->{
            String id=texto(r,campo);
            if(id==null)throw new IllegalStateException("Identificador ausente em "+tabela);
            var anterior=indice.putIfAbsent(id,r);
            if(anterior!=null)throw new IllegalStateException("Identificador conflitante em "+tabela);
        });
        return indice;
    }
    public static String texto(Map<String,Object> r,String campo){
        Object v=r.get(campo);if(v==null)return null;String s=v.toString().trim();return s.isEmpty()?null:s;
    }
}
