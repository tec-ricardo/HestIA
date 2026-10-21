package br.com.hestia.microtreinamento.service;

import br.com.hestia.microtreinamento.dto.OcorrenciaErroDTO;
import br.com.hestia.microtreinamento.model.CriterioErroRecorrente;
import br.com.hestia.microtreinamento.model.TipoErro;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class FonteRecorrenciaConformidade {

    private final JdbcTemplate jdbc;

    public FonteRecorrenciaConformidade(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional(readOnly = true)
    public Leitura consultar(
            Long empresaId,
            Long usuarioId,
            TipoErro tipoErro,
            LocalDateTime conclusao,
            LocalDateTime observadoAte
    ) {
        if (empresaId == null || empresaId <= 0
                || usuarioId == null || usuarioId <= 0
                || tipoErro == null || conclusao == null
                || observadoAte == null
                || observadoAte.isBefore(conclusao)) {
            throw new IllegalArgumentException(
                    "Parâmetros da fonte de recorrência inválidos."
            );
        }

        String criterio = switch (tipoErro) {
            case FERRAMENTA_NAO_AUTORIZADA -> "IA_AUTORIZADA";
            case VIOLACAO_POLITICA_INTERNA -> "POLITICA_INTERNA";
            default -> null;
        };

        if (criterio == null) {
            return new Leitura(
                    List.of(),
                    new Janela(0, 0),
                    new Janela(0, 0),
                    false
            );
        }

        int dias = CriterioErroRecorrente.PERIODO_DIAS;

        var linhas = jdbc.query(
                """
                SELECT r.id AS registro_id,
                       r.data_hora,
                       a.id AS avaliacao_id,
                       a.data_avaliacao,
                       a.status,
                       a.regra_codigo,
                       a.regra_versao,
                       a.politica_uso_id AS politica_avaliada,
                       v.politica_uso_id AS politica_historica,
                       v.empresa_id AS empresa_politica,
                       EXISTS (
                           SELECT 1
                           FROM avaliacao_criterios_atendidos c
                           WHERE c.avaliacao_id = a.id
                             AND c.criterio = ?
                       ) AS criterio_atendido
                FROM registros_uso_ia r
                JOIN usuarios u ON u.id = r.usuario_id
                LEFT JOIN avaliacoes_conformidade a
                    ON a.registro_uso_id = r.id
                LEFT JOIN politica_uso_versoes v
                    ON v.id = a.politica_versao_id
                WHERE u.empresa_id = ?
                  AND u.id = ?
                  AND r.data_hora >= ?
                  AND r.data_hora < ?
                  AND r.data_hora <= ?
                ORDER BY r.data_hora, r.id
                """,
                (rs, numero) -> {
                    Timestamp dataAvaliacao =
                            rs.getTimestamp("data_avaliacao");

                    return new Linha(
                            rs.getLong("registro_id"),
                            rs.getTimestamp("data_hora").toLocalDateTime(),
                            rs.getObject("avaliacao_id", Long.class),
                            dataAvaliacao == null
                                    ? null
                                    : dataAvaliacao.toLocalDateTime(),
                            rs.getString("status"),
                            rs.getString("regra_codigo"),
                            rs.getString("regra_versao"),
                            rs.getObject("politica_avaliada", Long.class),
                            rs.getObject("politica_historica", Long.class),
                            rs.getObject("empresa_politica", Long.class),
                            rs.getBoolean("criterio_atendido")
                    );
                },
                criterio,
                empresaId,
                usuarioId,
                Timestamp.valueOf(conclusao.minusDays(dias)),
                Timestamp.valueOf(conclusao.plusDays(dias)),
                Timestamp.valueOf(observadoAte)
        );

        long registrosAntes = 0;
        long registrosDepois = 0;
        long avaliadosAntes = 0;
        long avaliadosDepois = 0;

        var ocorrencias = new ArrayList<OcorrenciaErroDTO>();

        for (Linha linha : linhas) {
            boolean antes = linha.data().isBefore(conclusao);

            if (antes) {
                registrosAntes++;
            } else {
                registrosDepois++;
            }

            if (!avaliacaoUtilizavel(linha, empresaId, observadoAte)) {
                continue;
            }

            if (antes) {
                avaliadosAntes++;
            } else {
                avaliadosDepois++;
            }

            if (!linha.criterioAtendido()) {
                ocorrencias.add(new OcorrenciaErroDTO(
                        "AVALIACAO:" + linha.avaliacaoId()
                                + ":" + tipoErro.name(),
                        empresaId,
                        usuarioId,
                        tipoErro,
                        linha.data()
                ));
            }
        }

        return new Leitura(
                ocorrencias,
                new Janela(registrosAntes, avaliadosAntes),
                new Janela(registrosDepois, avaliadosDepois),
                true
        );
    }

    private boolean avaliacaoUtilizavel(
            Linha linha,
            Long empresaId,
            LocalDateTime observadoAte
    ) {
        if (linha.avaliacaoId() == null
                || linha.dataAvaliacao() == null
                || linha.dataAvaliacao().isAfter(observadoAte)
                || linha.dataAvaliacao().isBefore(linha.data())
                || !"CONFORMIDADE_CRITERIOS".equals(linha.regraCodigo())
                || !"1.0".equals(linha.regraVersao())
                || !empresaId.equals(linha.empresaPolitica())
                || linha.politicaAvaliada() == null
                || !linha.politicaAvaliada().equals(
                linha.politicaHistorica()
        )) {
            return false;
        }

        return switch (linha.status() == null ? "" : linha.status()) {
            case "CONFORME", "REQUER_REVISAO" ->
                    linha.criterioAtendido();
            case "NAO_CONFORME" -> true;
            default -> false;
        };
    }

    public record Janela(
            long registros,
            long avaliados
    ) {
        public boolean completa() {
            return registros > 0 && registros == avaliados;
        }
    }

    public record Leitura(
            List<OcorrenciaErroDTO> ocorrencias,
            Janela antes,
            Janela depois,
            boolean tipoSuportado
    ) {
        public Leitura {
            ocorrencias = List.copyOf(ocorrencias);
        }

        public boolean coberturaConfirmada() {
            return tipoSuportado
                    && antes.completa()
                    && depois.completa();
        }
    }

    private record Linha(
            Long registroId,
            LocalDateTime data,
            Long avaliacaoId,
            LocalDateTime dataAvaliacao,
            String status,
            String regraCodigo,
            String regraVersao,
            Long politicaAvaliada,
            Long politicaHistorica,
            Long empresaPolitica,
            boolean criterioAtendido
    ) {
    }
}