package br.com.hestia.indicadores.service;

import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

/** Consolida notas já calculadas por fontes autorizadas. Não cria notas a partir de dados brutos. */
@Service
public class CalculoMaturidadeService {
    private final ComposicaoMaturidadeService composicao;

    public CalculoMaturidadeService(ComposicaoMaturidadeService composicao) {
        this.composicao = composicao;
    }

    public record Parcela(String codigo, BigDecimal peso, BigDecimal nota, BigDecimal contribuicao) {}
    public record Resultado(String versaoRegra, String status, BigDecimal score,
                            BigDecimal pesoComNota, List<String> fatoresPendentes, List<Parcela> parcelas) {}

    public Resultado calcular(Map<String, BigDecimal> notas) {
        if (notas == null) throw new IllegalArgumentException("O mapa de notas é obrigatório");
        var regra = composicao.consultar();
        var codigos = regra.fatores().stream().map(f -> f.codigo()).toList();
        if (!codigos.containsAll(notas.keySet())) {
            throw new IllegalArgumentException("O mapa contém fatores desconhecidos");
        }
        List<Parcela> parcelas = new ArrayList<>();
        List<String> pendentes = new ArrayList<>();
        BigDecimal soma = BigDecimal.ZERO;
        BigDecimal cobertura = BigDecimal.ZERO;
        for (var fator : regra.fatores()) {
            BigDecimal nota = notas.get(fator.codigo());
            if (nota == null) {
                pendentes.add(fator.codigo());
                parcelas.add(new Parcela(fator.codigo(), fator.peso(), null, null));
                continue;
            }
            if (nota.signum() < 0 || nota.compareTo(BigDecimal.valueOf(100)) > 0) {
                throw new IllegalArgumentException("A nota de " + fator.codigo() + " deve estar entre 0 e 100");
            }
            BigDecimal contribuicao = nota.multiply(fator.peso());
            soma = soma.add(contribuicao);
            cobertura = cobertura.add(fator.peso());
            parcelas.add(new Parcela(fator.codigo(), fator.peso(), nota, contribuicao));
        }
        return new Resultado(regra.versaoRegra(), pendentes.isEmpty() ? "CALCULADO" : "FATORES_INSUFICIENTES",
                pendentes.isEmpty() ? soma : null, cobertura, List.copyOf(pendentes), List.copyOf(parcelas));
    }
}
