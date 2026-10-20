package br.com.hestia.indicadores.service;

import br.com.hestia.indicadores.dto.ComposicaoMaturidadeDTO;
import br.com.hestia.indicadores.dto.FatorMaturidadeDTO;
import br.com.hestia.indicadores.dto.PeriodoReferenciaDTO;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class ComposicaoMaturidadeService {

    private static final String VERSAO_ATUAL = "1.0";

    private static final List<FatorMaturidadeDTO> FATORES_V1 = List.of(
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
        return consultar(null, null, VERSAO_ATUAL);
    }

    public ComposicaoMaturidadeDTO consultar(
            LocalDate inicio,
            LocalDate fim,
            String versao
    ) {
        String versaoSolicitada = versao == null
                ? VERSAO_ATUAL
                : versao;

        if (!VERSAO_ATUAL.equals(versaoSolicitada)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Versão da regra não encontrada."
            );
        }

        if ((inicio == null) != (fim == null)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Informe início e fim do período."
            );
        }

        if (inicio != null && inicio.isAfter(fim)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "O início não pode ser posterior ao fim."
            );
        }

        PeriodoReferenciaDTO periodo = inicio == null
                ? null
                : new PeriodoReferenciaDTO(inicio, fim);

        BigDecimal soma = FATORES_V1.stream()
                .map(FatorMaturidadeDTO::peso)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (soma.compareTo(BigDecimal.ONE) != 0) {
            throw new IllegalStateException(
                    "A soma dos pesos de maturidade deve ser igual a um."
            );
        }

        return new ComposicaoMaturidadeDTO(
                versaoSolicitada,
                periodo,
                0,
                100,
                "AI Maturity Score = soma(nota do fator × peso do fator)",
                soma,
                FATORES_V1
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