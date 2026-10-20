package br.com.hestia.indicadores.service;

import br.com.hestia.indicadores.dto.ComposicaoMaturidadeDTO;
import br.com.hestia.indicadores.dto.FatorMaturidadeDTO;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class ComposicaoMaturidadeService {

    private static final List<FatorMaturidadeDTO> FATORES = List.of(
            fator("aiLiteracy", "AI Literacy", "0.15"),
            fator("governanca", "Governança", "0.15"),
            fator("usoResponsavel", "Uso Responsável", "0.15"),
            fator("produtividade", "Produtividade", "0.10"),
            fator("finOps", "FinOps", "0.10"),
            fator("seguranca", "Segurança", "0.20"),
            fator("colaboracao", "Colaboração", "0.05"),
            fator(
                    "desenvolvimentoCompetencias",
                    "Desenvolvimento de Competências",
                    "0.10"
            )
    );

    public ComposicaoMaturidadeDTO consultar() {
        BigDecimal soma = FATORES.stream()
                .map(FatorMaturidadeDTO::peso)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (soma.compareTo(BigDecimal.ONE) != 0) {
            throw new IllegalStateException(
                    "A soma dos pesos de maturidade deve ser igual a um."
            );
        }

        return new ComposicaoMaturidadeDTO(
                0,
                100,
                "AI Maturity Score = soma(nota do fator × peso do fator)",
                soma,
                FATORES
        );
    }

    private static FatorMaturidadeDTO fator(
            String codigo,
            String nome,
            String peso
    ) {
        return new FatorMaturidadeDTO(
                codigo, nome, new BigDecimal(peso)
        );
    }
}