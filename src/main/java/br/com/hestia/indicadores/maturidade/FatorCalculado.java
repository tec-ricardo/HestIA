package br.com.hestia.indicadores.maturidade;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record FatorCalculado(String codigo, BigDecimal peso, BigDecimal nota, String estado,
        boolean elegivelGlobal, String motivo, BigDecimal coberturaTecnica,
        BigDecimal coberturaClassificacao, long amostra, Long populacao,
        BigDecimal numerador, BigDecimal denominador, String formula, String fonte,
        Map<String, BigDecimal> estatisticas, List<String> avisos, String versaoCalculo,
        String naturezaDados, List<String> campos, String coorte) {
    public FatorCalculado(String codigo, BigDecimal peso, BigDecimal nota, String estado,
            boolean elegivelGlobal, String motivo, BigDecimal coberturaTecnica,
            BigDecimal coberturaClassificacao, long amostra, Long populacao,
            BigDecimal numerador, BigDecimal denominador, String formula, String fonte,
            Map<String, BigDecimal> estatisticas, List<String> avisos, String versaoCalculo) {
        this(codigo,peso,nota,estado,elegivelGlobal,motivo,coberturaTecnica,coberturaClassificacao,
            amostra,populacao,numerador,denominador,formula,fonte,estatisticas,avisos,versaoCalculo,null,
            camposDoFator(codigo),coorteDoFator(codigo));
    }
    public FatorCalculado comNatureza(String natureza) {
        return new FatorCalculado(codigo,peso,nota,estado,elegivelGlobal,motivo,coberturaTecnica,
            coberturaClassificacao,amostra,populacao,numerador,denominador,formula,fonte,estatisticas,
            avisos,versaoCalculo,natureza,campos,coorte);
    }
    private static List<String> camposDoFator(String codigo) {
        return switch(codigo) {
            case "aiLiteracy", "desenvolvimentoCompetencias" -> List.of("employee_id","course_category","score","enrollment_date","completion_date","completion_status");
            case "governanca" -> List.of("usage_id","employee_id","department_id","ai_tool_id","timestamp","request_count","approval_status","approval_date","allowed_departments");
            case "usoResponsavel" -> List.of("data_avaliacao","status","politica_versao_id","regra_codigo","regra_versao","motivo_preservado","regra_preservada","limitacoes_preservadas");
            case "produtividade" -> List.of("project_id","employee_id","issue_type","created_at","closed_at");
            case "finOps" -> List.of("company_id","cost_center","month","total_cost","currency","budget_amount");
            case "seguranca" -> List.of("controle","aplicabilidade","evidencia","data","resultado","versaoCatalogo");
            case "colaboracao" -> List.of("project_id","employee_id","created_at","merged_at","review_count");
            default -> throw new IllegalArgumentException("Fator desconhecido");
        };
    }
    private static String coorteDoFator(String codigo) {
        return switch(codigo) {
            case "aiLiteracy" -> "Pessoas com conclusões válidas de IA Generativa no período";
            case "desenvolvimentoCompetencias" -> "Pessoas com matrícula elegível no período e resultado até o corte";
            case "governanca" -> "Linhas de uso distintas; nota ponderada por requests";
            case "produtividade" -> "Issues fechadas de um único tipo no período";
            case "finOps" -> "Centros de custo e meses calendários completos";
            case "seguranca" -> "Universo de controles obrigatórios aplicáveis";
            case "colaboracao" -> "PRs integrados no período, por autoria autorizada";
            case "usoResponsavel" -> "Avaliações por data_avaliacao e registros do escopo operacional";
            default -> throw new IllegalArgumentException("Fator desconhecido");
        };
    }
    public FatorCalculado {
        estatisticas = Map.copyOf(estatisticas);
        avisos = List.copyOf(avisos);
        campos = List.copyOf(campos);
    }
}
