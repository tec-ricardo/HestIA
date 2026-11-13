package br.com.hestia.indicadores.maturidade;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class EvidenciasAiLiteracyRepositoryTest {

    @Test
    void preservaTentativasDaMesmaVersaoEAplicaEscopoPeriodoEVigencia() {
        var jdbc = new JdbcTemplate(new DriverManagerDataSource(
                "jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1", "sa", ""));
        jdbc.execute("CREATE TABLE usuarios(id BIGINT, empresa_id BIGINT, departamento_id BIGINT, ativo BOOLEAN)");
        jdbc.execute("CREATE TABLE aprendizagem_ciclos(id BIGINT, usuario_id BIGINT, empresa_id BIGINT, catalogo_id BIGINT)");
        jdbc.execute("CREATE TABLE aprendizagem_tentativas(id BIGINT, ciclo_id BIGINT, nota NUMERIC(5,2), status VARCHAR, finalizada_em TIMESTAMP)");
        jdbc.execute("CREATE TABLE aprendizagem_catalogo(id BIGINT, empresa_id BIGINT, treinamento_id BIGINT, versao INT, conteudo_sha256 VARCHAR, tema VARCHAR, publicado_em TIMESTAMP)");
        jdbc.execute("CREATE TABLE catalogo_treinamentos(id BIGINT, empresa_id BIGINT, conteudo_sha256 VARCHAR, tema VARCHAR, publicado_em TIMESTAMP, estado VARCHAR, retirado_em TIMESTAMP)");

        jdbc.update("INSERT INTO usuarios VALUES (1, 1, 10, TRUE), (2, 1, 20, TRUE), (3, 2, 10, TRUE)");
        jdbc.update("INSERT INTO aprendizagem_ciclos VALUES (1, 1, 1, 1), (2, 2, 1, 1), (3, 3, 2, 2)");
        jdbc.update("""
                INSERT INTO aprendizagem_catalogo VALUES
                (1, 1, 100, 2, 'hash1', 'tema', TIMESTAMP '2026-09-01 00:00:00'),
                (2, 2, 200, 1, 'hash2', 'tema', TIMESTAMP '2026-09-01 00:00:00')
                """);
        jdbc.update("""
                INSERT INTO catalogo_treinamentos VALUES
                (100, 1, 'hash1', 'tema', TIMESTAMP '2026-09-01 00:00:00', 'RETIRADO', TIMESTAMP '2026-10-20 00:00:00'),
                (200, 2, 'hash2', 'tema', TIMESTAMP '2026-09-01 00:00:00', 'PUBLICADO', NULL)
                """);
        jdbc.update("""
                INSERT INTO aprendizagem_tentativas VALUES
                (1, 1, 40, 'REPROVADA', TIMESTAMP '2026-10-01 00:00:00'),
                (2, 1, 80, 'APROVADA', TIMESTAMP '2026-10-10 10:00:00'),
                (3, 1, 100, 'APROVADA', TIMESTAMP '2026-10-20 00:00:00'),
                (4, 1, 100, 'APROVADA', TIMESTAMP '2026-09-30 23:59:59'),
                (5, 2, 100, 'APROVADA', TIMESTAMP '2026-10-10 10:00:00'),
                (6, 3, 100, 'APROVADA', TIMESTAMP '2026-10-10 10:00:00'),
                (7, 1, NULL, 'EM_ANDAMENTO', NULL)
                """);

        var evidencias = new EvidenciasAiLiteracyRepository(jdbc).buscar(
                1L, 10L, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31));

        assertThat(evidencias).extracting(EvidenciasAiLiteracyRepository.Tentativa::tentativaId)
                .containsExactly(1L, 2L);
        assertThat(evidencias).extracting(EvidenciasAiLiteracyRepository.Tentativa::versaoAvaliacao)
                .containsExactly(2, 2);
        assertThat(evidencias).extracting(EvidenciasAiLiteracyRepository.Tentativa::status)
                .containsExactly("REPROVADA", "APROVADA");
    }
}
