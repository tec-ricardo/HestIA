package br.com.hestia.indicadores.service;

import br.com.hestia.indicadores.dto.CoberturaFatorDTO;
import br.com.hestia.indicadores.dto.CoberturaMaturidadeDTO;
import br.com.hestia.indicadores.dto.ComposicaoMaturidadeDTO;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CoberturaMaturidadeService {

    public CoberturaMaturidadeDTO calcular(
            ComposicaoMaturidadeDTO composicao,
            Map<String, BigDecimal> notas
    ) {
        if (composicao == null || composicao.fatores().isEmpty()) {
            throw new IllegalArgumentException(
                    "Informe uma composição com fatores."
            );
        }

        if (notas == null) {
            throw new IllegalArgumentException(
                    "Informe o mapa de notas, mesmo que vazio."
            );
        }

        Set<String> codigos = composicao.fatores().stream()
                .map(fator -> fator.codigo())
                .collect(Collectors.toSet());

        if (codigos.size() != composicao.fatores().size()) {
            throw new IllegalArgumentException(
                    "A composição contém fatores duplicados."
            );
        }

        if (!codigos.containsAll(notas.keySet())) {
            throw new IllegalArgumentException(
                    "Existem notas de fatores não previstos na regra."
            );
        }

        BigDecimal somaPesos = composicao.fatores().stream()
                .map(fator -> fator.peso())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (somaPesos.compareTo(BigDecimal.ONE) != 0) {
            throw new IllegalArgumentException(
                    "A soma dos pesos deve ser igual a um."
            );
        }

        var fatores = new ArrayList<CoberturaFatorDTO>();
        int disponiveis = 0;
        BigDecimal somaPonderada = BigDecimal.ZERO;

        BigDecimal minimo = BigDecimal.valueOf(composicao.notaMinima());
        BigDecimal maximo = BigDecimal.valueOf(composicao.notaMaxima());

        for (var fator : composicao.fatores()) {
            if (fator.peso().signum() < 0) {
                throw new IllegalArgumentException(
                        "O peso não pode ser negativo."
                );
            }

            BigDecimal nota = notas.get(fator.codigo());
            boolean disponivel = nota != null;

            if (disponivel) {
                if (nota.compareTo(minimo) < 0
                        || nota.compareTo(maximo) > 0) {
                    throw new IllegalArgumentException(
                            "Nota fora da escala para " + fator.codigo() + "."
                    );
                }

                disponiveis++;
                somaPonderada = somaPonderada.add(
                        nota.multiply(fator.peso())
                );
            }

            fatores.add(new CoberturaFatorDTO(
                    fator.codigo(),
                    fator.nome(),
                    fator.peso(),
                    nota,
                    disponivel,
                    disponivel ? null : "Nota calculável não informada."
            ));
        }

        int total = fatores.size();

        BigDecimal cobertura = BigDecimal.valueOf(disponiveis)
                .multiply(BigDecimal.valueOf(100))
                .divide(
                        BigDecimal.valueOf(total),
                        2,
                        RoundingMode.HALF_UP
                );

        BigDecimal score = disponiveis == total
                ? somaPonderada.setScale(2, RoundingMode.HALF_UP)
                : null;

        return new CoberturaMaturidadeDTO(
                composicao.versaoRegra(),
                composicao.periodoReferencia(),
                total,
                disponiveis,
                cobertura,
                score,
                fatores
        );
    }
}