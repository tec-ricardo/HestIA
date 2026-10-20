package br.com.hestia.conformidade.dto;

import br.com.hestia.conformidade.model.CriterioConformidade;
import br.com.hestia.conformidade.model.StatusConformidade;

import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

public record FundamentosConformidade(
        String motivo,
        String regraAplicada,
        String limitacoes
) {
    public static FundamentosConformidade from(StatusConformidade status,
                                                Set<CriterioConformidade> atendidos) {
        Set<CriterioConformidade> faltantes = EnumSet.allOf(CriterioConformidade.class);
        faltantes.removeAll(atendidos);

        String criteriosFaltantes = faltantes.isEmpty()
                ? "nenhum"
                : faltantes.stream()
                        .map(FundamentosConformidade::descricao)
                        .sorted()
                        .collect(Collectors.joining(", "));

        String motivo = switch (status) {
            case CONFORME -> "Todos os critérios de conformidade foram atendidos.";
            case NAO_CONFORME -> "A avaliação possui critérios críticos não atendidos: "
                    + criteriosFaltantes + ".";
            case REQUER_REVISAO -> "A avaliação não apresentou falha crítica, mas ainda possui critérios pendentes: "
                    + criteriosFaltantes + ".";
        };

        String regra = "CONFORME exige o atendimento de todos os critérios. "
                + "NAO_CONFORME é aplicado quando falta autorização da IA, política interna ativa ou requisito LGPD. "
                + "Nos demais casos incompletos, o resultado é REQUER_REVISAO.";

        String limitacoes = "A avaliação é baseada nos dados registrados e na política selecionada. "
                + "Ela não substitui a revisão humana nem valida informações externas ao HestIA."
                + (faltantes.isEmpty() ? "" : " Critérios pendentes: " + criteriosFaltantes + ".");

        return new FundamentosConformidade(motivo, regra, limitacoes);
    }

    private static String descricao(CriterioConformidade criterio) {
        return switch (criterio) {
            case DADOS_SENSIVEIS -> "dados sensíveis";
            case IA_AUTORIZADA -> "IA autorizada";
            case POLITICA_INTERNA -> "política interna";
            case FINALIDADE_ADEQUADA -> "finalidade adequada";
            case VALIDACAO_HUMANA -> "validação humana";
            case REGISTRO_DE_USO -> "registro de uso";
            case LGPD -> "LGPD";
        };
    }
}
