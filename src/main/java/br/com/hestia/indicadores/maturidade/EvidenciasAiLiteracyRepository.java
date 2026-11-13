package br.com.hestia.indicadores.maturidade;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public class EvidenciasAiLiteracyRepository {

    private final JdbcTemplate jdbc;

    public EvidenciasAiLiteracyRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public record Tentativa(
            long tentativaId,
            long usuarioId,
            long avaliacaoId,
            long treinamentoId,
            int versaoAvaliacao,
            BigDecimal nota,
            String status,
            LocalDateTime finalizadaEm
    ) {
    }

    List<Tentativa> buscar(Long empresaId, Long departamentoId, LocalDate inicio, LocalDate fim) {
        return jdbc.query("""
                SELECT t.id, u.id AS usuario_id, a.id AS avaliacao_id, a.treinamento_id,
                       a.versao, t.nota, t.status, t.finalizada_em
                FROM aprendizagem_tentativas t
                JOIN aprendizagem_ciclos c ON c.id = t.ciclo_id
                JOIN usuarios u ON u.id = c.usuario_id AND u.empresa_id = c.empresa_id
                JOIN aprendizagem_catalogo a ON a.id = c.catalogo_id AND a.empresa_id = c.empresa_id
                JOIN catalogo_treinamentos treino ON treino.id = a.treinamento_id
                    AND treino.empresa_id = a.empresa_id
                    AND treino.conteudo_sha256 = a.conteudo_sha256 AND treino.tema = a.tema
                WHERE u.empresa_id = ? AND u.ativo = TRUE
                  AND (CAST(? AS BIGINT) IS NULL OR u.departamento_id = ?)
                  AND t.finalizada_em >= ? AND t.finalizada_em < ?
                  AND t.status IN ('APROVADA', 'REPROVADA')
                  AND t.nota BETWEEN 0 AND 100
                  AND a.publicado_em <= t.finalizada_em
                  AND treino.publicado_em <= t.finalizada_em
                  AND (treino.estado = 'PUBLICADO'
                      OR (treino.estado = 'RETIRADO' AND treino.retirado_em > t.finalizada_em))
                ORDER BY u.id, a.id, t.finalizada_em, t.id
                """, (rs, linha) -> new Tentativa(
                        rs.getLong("id"), rs.getLong("usuario_id"), rs.getLong("avaliacao_id"),
                        rs.getLong("treinamento_id"), rs.getInt("versao"), rs.getBigDecimal("nota"),
                        rs.getString("status"), rs.getTimestamp("finalizada_em").toLocalDateTime()),
                empresaId, departamentoId, departamentoId,
                Timestamp.valueOf(inicio.atStartOfDay()), Timestamp.valueOf(fim.plusDays(1).atStartOfDay()));
    }
}
