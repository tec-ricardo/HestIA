package br.com.hestia.microtreinamento.service;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class EfetividadeCapacitacaoService {

    public Resultado avaliar(
            RecorrenciaCapacitacaoService.Resultado acompanhamento,
            boolean coberturaConfirmada
    ) {
        if (acompanhamento == null
                || acompanhamento.recorrencia() == null) {
            throw new IllegalArgumentException(
                    "Informe o acompanhamento da capacitação."
            );
        }

        var recorrencia = acompanhamento.recorrencia();

        if (recorrencia.ocorrenciasAntes() < 0
                || recorrencia.ocorrenciasDepois() < 0) {
            throw new IllegalArgumentException(
                    "As quantidades de ocorrências não podem ser negativas."
            );
        }

        if (!coberturaConfirmada) {
            return insuficiente(
                    acompanhamento,
                    Motivo.COBERTURA_NAO_CONFIRMADA
            );
        }

        if (!recorrencia.periodoPosteriorCompleto()) {
            return insuficiente(
                    acompanhamento,
                    Motivo.PERIODO_POSTERIOR_INCOMPLETO
            );
        }

        long antes = recorrencia.ocorrenciasAntes();
        long depois = recorrencia.ocorrenciasDepois();

        if (antes == 0) {
            return insuficiente(
                    acompanhamento,
                    Motivo.SEM_OCORRENCIAS_ANTERIORES
            );
        }

        Situacao situacao = depois < antes
                ? Situacao.MELHORIA
                : depois > antes
                ? Situacao.PIORA
                : Situacao.ESTABILIDADE;

        BigDecimal reducaoPercentual = BigDecimal.valueOf(antes)
                .subtract(BigDecimal.valueOf(depois))
                .multiply(BigDecimal.valueOf(100))
                .divide(
                        BigDecimal.valueOf(antes),
                        2,
                        RoundingMode.HALF_UP
                );

        return new Resultado(
                acompanhamento,
                situacao,
                Motivo.COMPARACAO_DISPONIVEL,
                reducaoPercentual
        );
    }

    private Resultado insuficiente(
            RecorrenciaCapacitacaoService.Resultado acompanhamento,
            Motivo motivo
    ) {
        return new Resultado(
                acompanhamento,
                Situacao.DADOS_INSUFICIENTES,
                motivo,
                null
        );
    }

    public enum Situacao {
        MELHORIA,
        ESTABILIDADE,
        PIORA,
        DADOS_INSUFICIENTES
    }

    public enum Motivo {
        COMPARACAO_DISPONIVEL,
        COBERTURA_NAO_CONFIRMADA,
        PERIODO_POSTERIOR_INCOMPLETO,
        SEM_OCORRENCIAS_ANTERIORES
    }

    public record Resultado(
            RecorrenciaCapacitacaoService.Resultado acompanhamento,
            Situacao situacao,
            Motivo motivo,
            BigDecimal reducaoPercentual
    ) {
    }
}