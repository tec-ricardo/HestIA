package br.com.hestia.indicadores.service;

import br.com.hestia.indicadores.dto.ConfiancaIndicadorDTO;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Service
public class RegraConfiancaService {
    public static final String VERSAO = "CONFIANCA-1.0";
    public static final int AMOSTRA_MINIMA = 30;
    public static final BigDecimal LIMITE = new BigDecimal("0.95");

    public ConfiancaIndicadorDTO avaliar(BigDecimal valor, long universo, long cobertos,
            long completos, long amostra, boolean estimativa) {
        if (universo < 0 || cobertos < 0 || cobertos > universo || completos < 0
                || completos > cobertos || amostra < 0 || amostra > completos
                || (valor != null && universo == 0))
            throw new IllegalArgumentException("Contagens incompatíveis na evidência do indicador.");
        BigDecimal cobertura = razao(cobertos, universo);
        BigDecimal completude = razao(completos, cobertos);
        var motivos = new ArrayList<String>();
        if (valor == null) {
            motivos.add(universo == 0 ? "Nenhum registro no período e escopo consultados."
                    : "Não há evidência conclusiva suficiente para calcular uma nota.");
            return resultado("ESTIMADO", "SEM_DADOS", cobertura, completude, amostra, universo, motivos);
        }
        if (estimativa) motivos.add("Valor estimado; não é uma medição definitiva.");
        // Compara contagens exatas: o arredondamento exibido nunca promove uma classificação.
        if (!atinge(cobertos, universo)) motivos.add("Cobertura inferior a 95% do universo observado.");
        if (!atinge(completos, cobertos)) motivos.add("Completude inferior a 95% dos registros cobertos.");
        if (amostra < AMOSTRA_MINIMA) motivos.add("Amostra inferior a 30 registros distintos.");
        String classe = estimativa ? "ESTIMADO" : motivos.isEmpty() ? "CONFIAVEL" : "PARCIAL";
        if (motivos.isEmpty()) motivos.add("Cobertura, completude e amostra atendem à regra vigente.");
        return resultado(classe, "CALCULADO", cobertura, completude, amostra, universo, motivos);
    }

    public ConfiancaIndicadorDTO indisponivel(String motivo) {
        return new ConfiancaIndicadorDTO("ESTIMADO", "FONTE_PENDENTE", VERSAO, null, null, 0, 0, null, null, List.of(motivo));
    }

    private boolean atinge(long n, long d) {
        return d > 0 && BigDecimal.valueOf(n).compareTo(BigDecimal.valueOf(d).multiply(LIMITE)) >= 0;
    }
    private BigDecimal razao(long n, long d) {
        return d == 0 ? null : BigDecimal.valueOf(n).divide(BigDecimal.valueOf(d), 6, RoundingMode.HALF_UP);
    }
    private ConfiancaIndicadorDTO resultado(String classe, String situacao, BigDecimal cobertura,
            BigDecimal completude, long amostra, long universo, List<String> motivos) {
        return new ConfiancaIndicadorDTO(classe, situacao, VERSAO, cobertura, completude,
                amostra, universo, AMOSTRA_MINIMA, LIMITE, motivos);
    }
}
