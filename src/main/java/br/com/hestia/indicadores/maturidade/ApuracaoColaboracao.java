package br.com.hestia.indicadores.maturidade;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class ApuracaoColaboracao {

    private final CalculadorasMaturidade calculadoras;

    public ApuracaoColaboracao(CalculadorasMaturidade calculadoras) {
        this.calculadoras = calculadoras;
    }

    public record PullRequest(
            String repositorio,
            long numero,
            LocalDate dataMerge,
            Long empresaId,
            Long departamentoId,
            Long autorId,
            Integer quantidadeRevisoes
    ) {
    }

    private record ChavePullRequest(String repositorio, long numero) {
    }

    public FatorCalculado calcular(
            Long empresaId,
            Long departamentoId,
            LocalDate inicio,
            LocalDate fim,
            List<PullRequest> registros
    ) {
        Objects.requireNonNull(empresaId, "Empresa obrigatória");
        if (fim.isBefore(inicio)) {
            throw new IllegalArgumentException("O fim do período deve ser igual ou posterior ao início");
        }

        Map<ChavePullRequest, Set<PullRequest>> porPullRequest = new HashMap<>();
        Set<ChavePullRequest> candidatos = new HashSet<>();

        for (PullRequest registro : registros) {
            if (!empresaId.equals(registro.empresaId())
                    || registro.repositorio() == null
                    || registro.repositorio().isBlank()
                    || registro.numero() <= 0) {
                continue;
            }

            var chave = new ChavePullRequest(registro.repositorio(), registro.numero());
            porPullRequest.computeIfAbsent(chave, ignorada -> new HashSet<>()).add(registro);

            LocalDate merge = registro.dataMerge();
            if (merge != null && !merge.isBefore(inicio) && !merge.isAfter(fim)
                    && (departamentoId == null || departamentoId.equals(registro.departamentoId()))) {
                candidatos.add(chave);
            }
        }

        long validos = 0;
        long revisados = 0;
        for (ChavePullRequest chave : candidatos) {
            Set<PullRequest> registrosDoPr = porPullRequest.get(chave);
            if (registrosDoPr.size() != 1) {
                continue;
            }

            PullRequest registro = registrosDoPr.iterator().next();
            Integer revisoes = registro.quantidadeRevisoes();
            if (registro.autorId() == null || registro.departamentoId() == null
                    || revisoes == null || revisoes < 0) {
                continue;
            }

            validos++;
            if (revisoes > 0) {
                revisados++;
            }
        }

        return calculadoras.colaboracao(revisados, validos, candidatos.size());
    }
}