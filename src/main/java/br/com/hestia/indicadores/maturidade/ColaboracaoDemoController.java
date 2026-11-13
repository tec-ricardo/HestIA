package br.com.hestia.indicadores.maturidade;

import br.com.hestia.reutilizavel.service.AcessoRespostaService;
import br.com.hestia.usuario.model.PerfilUsuario;
import br.com.hestia.usuario.model.Usuario;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;

@RestController
@Profile("demo")
@ConditionalOnProperty(name = "hestia.colaboracao.demo.arquivo")
public class ColaboracaoDemoController {

    private static final String NATUREZA = "EVENTOS_GITHUB_REAIS_COM_VINCULOS_DEMO";

    private final AcessoRespostaService acesso;
    private final ApuracaoColaboracao apuracao;
    private final Path arquivo;
    private final ObjectMapper json = new ObjectMapper();

    public ColaboracaoDemoController(AcessoRespostaService acesso, CalculadorasMaturidade calculadoras,
                                     @Value("${hestia.colaboracao.demo.arquivo}") String arquivo) {
        this.acesso = acesso;
        this.apuracao = new ApuracaoColaboracao(calculadoras);
        this.arquivo = Path.of(arquivo);
    }

    @GetMapping("/indicadores/maturidade/colaboracao-demo")
    public Resultado consultar(@RequestAttribute("usuarioAutenticado") Usuario usuario) {
        Long empresa = acesso.validarEmpresa(usuario, null);
        var departamento = usuario.getDepartamento();
        if (usuario.getPerfil() != PerfilUsuario.ADMIN || usuario.getEmpresa() == null
                || !"99999999000272".equals(usuario.getEmpresa().getCnpj())
                || departamento == null || departamento.getEmpresa() == null
                || !empresa.equals(departamento.getEmpresa().getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Consulta restrita à empresa GitHub demo.");
        }

        try {
            var fonte = json.readTree(arquivo.toFile());
            var registros = fonte.path("pullRequests");
            if (!NATUREZA.equals(fonte.path("natureza").asText()) || !registros.isArray() || registros.isEmpty()) {
                throw new IllegalArgumentException("Fonte demo inválida");
            }

            var inicio = LocalDate.parse(fonte.path("inicio").asText());
            var fim = LocalDate.parse(fonte.path("fim").asText());
            var prs = new ArrayList<ApuracaoColaboracao.PullRequest>();
            for (var pr : registros) {
                if (pr.path("empresaId").asLong() != empresa
                        || pr.path("departamentoId").asLong() != departamento.getId()) {
                    throw new IllegalArgumentException("Vínculos incompatíveis com este banco");
                }
                var revisoes = pr.path("quantidadeRevisoes");
                prs.add(new ApuracaoColaboracao.PullRequest(
                        pr.path("repositorio").asText(), pr.path("numero").asLong(),
                        LocalDate.parse(pr.path("dataMerge").asText()), empresa, departamento.getId(),
                        pr.path("autorId").isIntegralNumber() && pr.path("autorId").asLong() > 0
                                ? pr.path("autorId").asLong() : null,
                        revisoes.isIntegralNumber() && revisoes.canConvertToInt() ? revisoes.asInt() : null));
            }

            var fator = apuracao.calcular(empresa, departamento.getId(), inicio, fim, prs).comNatureza(NATUREZA);
            boolean fragil = !fonte.path("populacaoPrsCompleta").asBoolean() || !fator.elegivelGlobal();
            return new Resultado(NATUREZA, inicio, fim, fragil, fator);
        } catch (IOException | IllegalArgumentException | java.time.DateTimeException ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Fonte de colaboração demo indisponível.", ex);
        }
    }

    public record Resultado(String natureza, LocalDate inicio, LocalDate fim, boolean fragil, FatorCalculado fator) {}
}
