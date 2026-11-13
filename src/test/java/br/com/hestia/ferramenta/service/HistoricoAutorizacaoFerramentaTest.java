package br.com.hestia.ferramenta.service;

import br.com.hestia.empresa.model.Empresa;
import br.com.hestia.empresa.repository.EmpresaRepository;
import br.com.hestia.ferramenta.model.StatusFerramentaIA;
import br.com.hestia.ferramenta.repository.FerramentaIARepository;
import br.com.hestia.usuario.model.PerfilUsuario;
import br.com.hestia.usuario.model.Usuario;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HistoricoAutorizacaoFerramentaTest {
    @Test
    void registraAprovacaoERetiradaComVigenciasContiguas() {
        var jdbc = new JdbcTemplate(new DriverManagerDataSource(
                "jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1", "sa", ""));
        jdbc.execute("CREATE TABLE ferramentas_ia(id BIGINT, empresa_id BIGINT, status VARCHAR, data_atualizacao TIMESTAMP)");
        jdbc.execute("""
                CREATE TABLE autorizacoes_ferramentas_ia(
                    empresa_id BIGINT, ferramenta_id BIGINT, status VARCHAR,
                    inicio_vigencia TIMESTAMP, fim_vigencia TIMESTAMP,
                    responsavel_id BIGINT, motivo VARCHAR
                )
                """);
        jdbc.update("INSERT INTO ferramentas_ia VALUES (7, 1, 'EM_ANALISE', NULL)");
        var empresa = mock(Empresa.class);
        when(empresa.getId()).thenReturn(1L);
        var autor = mock(Usuario.class);
        when(autor.getId()).thenReturn(42L);
        when(autor.getAtivo()).thenReturn(true);
        when(autor.getEmpresa()).thenReturn(empresa);
        when(autor.getPerfil()).thenReturn(PerfilUsuario.ADMIN);
        var service = new FerramentaIAService(
                mock(FerramentaIARepository.class), mock(EmpresaRepository.class), jdbc);

        // A resolução do relógio no Windows pode produzir o mesmo instante em duas chamadas.
        // Este cenário verifica decisões em instantes distintos, sem depender da velocidade da máquina.
        var primeira = java.time.LocalDateTime.of(2026, 11, 3, 10, 0);
        var segunda = primeira.plusSeconds(1);
        try (var relogio = org.mockito.Mockito.mockStatic(java.time.LocalDateTime.class,
                org.mockito.Mockito.CALLS_REAL_METHODS)) {
            relogio.when(java.time.LocalDateTime::now).thenReturn(primeira, segunda);
            service.registrarAutorizacao(autor, 7L, StatusFerramentaIA.APROVADA, "Aprovação");
            service.registrarAutorizacao(autor, 7L, StatusFerramentaIA.DESCONTINUADA, "Retirada");
        }

        var historico = jdbc.queryForList("SELECT * FROM autorizacoes_ferramentas_ia ORDER BY inicio_vigencia");
        assertThat(historico).hasSize(2);
        assertThat(historico.get(0).get("status")).isEqualTo("APROVADA");
        assertThat(historico.get(0).get("fim_vigencia")).isEqualTo(historico.get(1).get("inicio_vigencia"));
        assertThat(historico.get(1).get("status")).isEqualTo("DESCONTINUADA");
        assertThat(historico.get(1).get("fim_vigencia")).isNull();
        assertThat(jdbc.queryForObject("SELECT status FROM ferramentas_ia WHERE id = 7", String.class))
                .isEqualTo("DESCONTINUADA");
    }
}
