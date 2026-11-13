package br.com.hestia.indicadores.maturidade;

import br.com.hestia.usuario.model.Usuario;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/indicadores/maturidade")
public class InvestigacaoMaturidadeController {

    private final ResultadoMaturidadeService resultados;
    private final JdbcTemplate jdbc;

    public InvestigacaoMaturidadeController(ResultadoMaturidadeService resultados, JdbcTemplate jdbc) {
        this.resultados = resultados;
        this.jdbc = jdbc;
    }

    @GetMapping("/investigacao")
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public Investigacao consultar(
            @RequestAttribute("usuarioAutenticado") Usuario usuario,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
            @RequestParam Long departamentoId) {
        var resultado = resultados.consultar(usuario, new ResultadoMaturidadeService.Consulta(
                inicio, fim, ResultadoMaturidadeService.Natureza.OPERACIONAL,
                null, departamentoId, null, null));

        var pendencias = jdbc.query("""
                SELECT t.id, t.avaliacao_id, t.status, t.responsavel_id
                FROM tratamentos_nao_conformidade t
                JOIN avaliacoes_conformidade a ON a.id = t.avaliacao_id
                JOIN registros_uso_ia r ON r.id = a.registro_uso_id
                WHERE t.empresa_id = ? AND t.departamento_id = ?
                  AND r.data_hora >= ? AND r.data_hora < ?
                  AND a.data_avaliacao >= ? AND a.data_avaliacao < ?
                  AND t.status IN ('ABERTA', 'EM_TRATAMENTO')
                ORDER BY t.id
                """, (rs, linha) -> new Pendencia(rs.getLong("id"), rs.getLong("avaliacao_id"),
                        rs.getString("status"), rs.getObject("responsavel_id", Long.class)),
                resultado.empresaId(), resultado.departamentoId(),
                Timestamp.valueOf(inicio.atStartOfDay()), Timestamp.valueOf(fim.plusDays(1).atStartOfDay()),
                Timestamp.valueOf(inicio.atStartOfDay()), Timestamp.valueOf(fim.plusDays(1).atStartOfDay()));

        return new Investigacao(resultado, pendencias);
    }

    public record Investigacao(ResultadoMaturidadeService.Resultado maturidade, List<Pendencia> pendencias) {}

    public record Pendencia(long tratamentoId, long avaliacaoId, String status, Long responsavelId) {}
}
