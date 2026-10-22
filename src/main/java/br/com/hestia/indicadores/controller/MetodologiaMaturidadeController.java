package br.com.hestia.indicadores.controller;

import br.com.hestia.indicadores.dto.ComposicaoMaturidadeDTO;
import br.com.hestia.indicadores.service.ComposicaoMaturidadeService;
import br.com.hestia.reutilizavel.service.AcessoRespostaService;
import br.com.hestia.usuario.model.Usuario;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/indicadores/maturidade")
public class MetodologiaMaturidadeController {
    private final ComposicaoMaturidadeService composicao;
    private final AcessoRespostaService acesso;

    public MetodologiaMaturidadeController(ComposicaoMaturidadeService composicao, AcessoRespostaService acesso) {
        this.composicao = composicao;
        this.acesso = acesso;
    }

    public record Fonte(String fator, String referencia, String fontePotencial, String situacao,String formula,String unidade,String dadosMinimos,String natureza) {}
    public record Metodologia(ComposicaoMaturidadeDTO composicao, String statusNotas,
                              String tratamentoAusencia, List<Fonte> fontes) {}

    @GetMapping("/metodologia")
    public Metodologia consultar(@RequestAttribute("usuarioAutenticado") Usuario usuario) {
        acesso.validarEmpresa(usuario, null);
        return new Metodologia(composicao.consultar(), "FORMULAS_EXPERIMENTAIS_PROPOSTA_0_1",
                "Score integral indisponível enquanto faltar nota; pesos não são redistribuídos",
                List.of(
                    fonte("aiLiteracy", "Conclusões e avaliações de aprendizagem"),
                    fonte("governanca", "Políticas, ferramentas aprovadas e avaliações de conformidade"),
                    fonte("usoResponsavel", "Critérios de conformidade e práticas de uso"),
                    fonte("produtividade", "Issues fechadas por tipo, projeto e período; meta de dias autorizada no servidor"),
                    fonte("finOps", "Custos e orçamento de centros/meses completos; moeda e conversão configuradas"),
                    fonte("seguranca", "Catálogo de controles aplicáveis e evidências; ausência de incidentes não substitui controle"),
                    fonte("colaboracao", "Pull requests integrados com contagem de revisões e autoria identificável"),
                    fonte("desenvolvimentoCompetencias", "Conclusões, avaliações, trilhas e certificações")
                ));
    }

    private Fonte fonte(String fator, String origem) {
        String formula=switch(fator){
            case "aiLiteracy"->"Média das médias de notas por pessoa em conclusões válidas de IA Generativa";
            case "governanca"->"100 * requests aprovados / requests classificados";
            case "usoResponsavel"->"100 * avaliações conformes / (conformes + não conformes)";
            case "produtividade"->"min(100, 100 * metaDias / medianaDias); mediana zero com meta positiva = 100";
            case "finOps"->"limitar(100 * (2 - custo / orçamento), 0, 100)";
            case "seguranca"->"100 * controles aprovados / controles avaliados; cobertura = avaliados / obrigatórios";
            case "colaboracao"->"100 * PRs com revisão / PRs válidos";
            case "desenvolvimentoCompetencias"->"100 * pessoas com aprovação qualificada / pessoas com resultado conhecido";
            default->throw new IllegalArgumentException("Fator desconhecido");};
        return new Fonte(fator, "HestIA_Especificacao_Metricas_Maturidade.docx: proposta 0.1", origem,
                "Implementação experimental proposta-0.1; fórmula e parâmetros são preservados nas apurações. Período com início inclusivo e fim no dia seguinte exclusivo; ausência nunca vira zero.",formula,"Nota derivada de 0 a 100",fator.equals("finOps")||fator.equals("seguranca")?"Universo completo, fonte identificada e denominador positivo":"Amostra mínima proposta de 30 unidades independentes e cobertura proposta >= 95%; valores menores são parciais","DERIVADO; natureza sintética ou operacional explícita por consulta");
    }
}
