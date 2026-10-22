package br.com.hestia.microtreinamento.service;

import br.com.hestia.microtreinamento.dto.OcorrenciaErroDTO;
import br.com.hestia.microtreinamento.model.CriterioErroRecorrente;
import br.com.hestia.microtreinamento.model.TipoErro;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class RecorrenciaErroService {

    public Resultado calcular(
            Long empresaId,
            Long usuarioId,
            TipoErro tipoErro,
            LocalDateTime dataConclusao,
            LocalDateTime observadoAte,
            List<OcorrenciaErroDTO> ocorrencias
    ) {
        if (empresaId == null || empresaId <= 0
                || usuarioId == null || usuarioId <= 0
                || tipoErro == null || dataConclusao == null
                || observadoAte == null || ocorrencias == null
                || observadoAte.isBefore(dataConclusao)) {
            throw new IllegalArgumentException(
                    "Parâmetros de recorrência inválidos."
            );
        }

        int dias = CriterioErroRecorrente.PERIODO_DIAS;
        LocalDateTime inicioAntes = dataConclusao.minusDays(dias);
        LocalDateTime fimDepois = dataConclusao.plusDays(dias);

        Map<String, OcorrenciaErroDTO> distintas = new HashMap<>();

        for (OcorrenciaErroDTO ocorrencia : ocorrencias) {
            if (ocorrencia == null) {
                throw new IllegalArgumentException(
                        "A lista contém ocorrência nula."
                );
            }

            if (!empresaId.equals(ocorrencia.empresaId())
                    || !usuarioId.equals(ocorrencia.usuarioId())
                    || tipoErro != ocorrencia.tipoErro()) {
                continue;
            }

            OcorrenciaErroDTO anterior = distintas.putIfAbsent(
                    ocorrencia.origemId(),
                    ocorrencia
            );

            if (anterior != null && !anterior.equals(ocorrencia)) {
                throw new IllegalArgumentException(
                        "Identificador de ocorrência com dados conflitantes."
                );
            }
        }

        long antes = distintas.values().stream()
                .map(OcorrenciaErroDTO::dataOcorrencia)
                .filter(data -> !data.isBefore(inicioAntes))
                .filter(data -> data.isBefore(dataConclusao))
                .count();

        long depois = distintas.values().stream()
                .map(OcorrenciaErroDTO::dataOcorrencia)
                .filter(data -> !data.isBefore(dataConclusao))
                .filter(data -> data.isBefore(fimDepois))
                .filter(data -> !data.isAfter(observadoAte))
                .count();

        boolean periodoPosteriorCompleto =
                !observadoAte.isBefore(fimDepois);

        return new Resultado(
                empresaId,
                usuarioId,
                tipoErro,
                inicioAntes,
                dataConclusao,
                fimDepois,
                observadoAte,
                antes,
                depois,
                antes >= CriterioErroRecorrente.QUANTIDADE_MINIMA_ERROS,
                depois >= CriterioErroRecorrente.QUANTIDADE_MINIMA_ERROS,
                periodoPosteriorCompleto
        );
    }

    public record Resultado(
            Long empresaId,
            Long usuarioId,
            TipoErro tipoErro,
            LocalDateTime inicioAntes,
            LocalDateTime dataConclusao,
            LocalDateTime fimDepois,
            LocalDateTime observadoAte,
            long ocorrenciasAntes,
            long ocorrenciasDepois,
            boolean recorrenteAntes,
            boolean recorrenteDepois,
            boolean periodoPosteriorCompleto
    ) {
    }
}