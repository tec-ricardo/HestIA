package br.com.hestia.indicadores.maturidade;

import org.springframework.stereotype.Component;

import java.util.List;

/** Traduz a elegibilidade já calculada em uma classificação legível e auditável. */
@Component
public class ClassificadorConfiancaMaturidade implements ConfiancaIndicadoresPort {
    public static final String VERSAO = "CONFIANCA-MATURIDADE-1.0";

    @Override
    public List<Classificacao> classificar(ResultadoMaturidadeService.Resultado resultado) {
        return resultado.fatores().stream().map(this::classificar).toList();
    }

    private Classificacao classificar(FatorCalculado fator) {
        if (fator.nota() == null) {
            return new Classificacao(fator.codigo(), "DADOS_INSUFICIENTES",
                    "A fonte ou a configuração necessária ainda não permite calcular esta dimensão.", VERSAO);
        }
        if (fator.elegivelGlobal()) {
            return new Classificacao(fator.codigo(), "CONFIAVEL",
                    "A amostra e a cobertura atendem aos critérios definidos para o índice.", VERSAO);
        }
        return new Classificacao(fator.codigo(), "PARCIAL",
                "Existe uma nota observada, mas a amostra ou a cobertura ainda não atende aos critérios do índice completo.", VERSAO);
    }
}
