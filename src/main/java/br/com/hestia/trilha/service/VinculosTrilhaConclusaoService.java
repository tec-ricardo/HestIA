package br.com.hestia.trilha.service;
import br.com.hestia.aprendizagem.AcessoCapacitacao;
import br.com.hestia.microtreinamento.service.VinculosTrilhaConclusaoPort;
import br.com.hestia.usuario.model.Usuario;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import java.util.*;
@Service
public class VinculosTrilhaConclusaoService implements VinculosTrilhaConclusaoPort {
    private final JdbcTemplate jdbc;private final AcessoCapacitacao acesso;
    public VinculosTrilhaConclusaoService(JdbcTemplate jdbc,AcessoCapacitacao acesso){this.jdbc=jdbc;this.acesso=acesso;}
    @Transactional(readOnly=true)
    public List<Map<String,Object>> consultar(Usuario autor,Long conclusaoId){
        var conclusoes=jdbc.queryForList("SELECT c.usuario_id FROM conclusoes_microtreinamento c JOIN usuarios u ON u.id=c.usuario_id WHERE c.id=? AND u.empresa_id=?",conclusaoId,acesso.empresa(autor));
        AcessoCapacitacao.exigir(!conclusoes.isEmpty(),HttpStatus.NOT_FOUND,"Conclusão indisponível");var u=acesso.usuario(autor,((Number)conclusoes.getFirst().get("usuario_id")).longValue());
        return jdbc.queryForList("SELECT p.id AS participacao_id,t.id AS trilha_id,t.titulo,p.usuario_id,p.empresa_id,p.status,p.iniciada_em,p.concluida_em,tc.microtreinamento_id,tc.conclusao_id,c.data_conclusao FROM trilha_conclusoes tc JOIN participacoes_trilha p ON p.id=tc.participacao_id JOIN trilhas t ON t.id=p.trilha_id JOIN conclusoes_microtreinamento c ON c.id=tc.conclusao_id WHERE tc.conclusao_id=? AND p.empresa_id=? AND p.usuario_id=? ORDER BY p.id",conclusaoId,acesso.empresa(autor),u.getId());
    }
}
