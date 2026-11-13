package br.com.hestia.indicadores.maturidade;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.EnumMap;
import java.util.List;
import java.util.Objects;

import static br.com.hestia.indicadores.maturidade.CatalogoControlesSeguranca.*;

@Component
@ConfigurationProperties(prefix = "hestia.maturidade.seguranca")
public class FonteEvidenciasSeguranca {

    private String release;
    private String ambiente;
    private List<Registro> evidencias = List.of();

    public String getRelease() { return release; }
    public void setRelease(String release) { this.release = release; }
    public String getAmbiente() { return ambiente; }
    public void setAmbiente(String ambiente) { this.ambiente = ambiente; }
    public List<Registro> getEvidencias() { return evidencias; }
    public void setEvidencias(List<Registro> evidencias) { this.evidencias = List.copyOf(evidencias); }

    public FatorCalculado calcular(Long empresa, Long departamento, LocalDate inicio, LocalDate fim) {
        if (vazio(release) || vazio(ambiente)) {
            return new CalculadorasMaturidade().pendente("seguranca", "CONFIGURACAO_PENDENTE",
                    "Release e ambiente avaliados não configurados", "Catálogo de controles e evidências");
        }
        var desde = inicio.atStartOfDay().toInstant(ZoneOffset.UTC);
        var ate = fim.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        var agora = Instant.now();
        var atuais = new EnumMap<Controle, Registro>(Controle.class);
        for (var r : evidencias) {
            if (!Objects.equals(empresa, r.empresaId()) || !Objects.equals(departamento, r.departamentoId())
                    || !release.equals(r.release()) || !ambiente.equals(r.ambiente())
                    || !VERSAO.equals(r.versaoCatalogo()) || r.controle() == null
                    || r.verificadaEm() == null || r.verificadaEm().isBefore(desde)
                    || !r.verificadaEm().isBefore(ate) || r.verificadaEm().isAfter(agora)) {
                continue;
            }
            atuais.merge(r.controle(), r, FonteEvidenciasSeguranca::maisRecente);
        }
        var provas = atuais.values().stream()
                .map(r -> new Evidencia(r.controle(),
                        r.situacao() == null || vazio(r.responsavel()) ? Situacao.NAO_VERIFICADO : r.situacao(),
                        r.referencia(), r.release(), r.verificadaEm()))
                .toList();
        var fator = avaliar(provas, release, agora);
        return new FatorCalculado(fator.codigo(), fator.peso(), fator.nota(), fator.estado(),
                fator.elegivelGlobal(), fator.motivo(), fator.coberturaTecnica(),
                fator.coberturaClassificacao(), fator.amostra(), fator.populacao(),
                fator.numerador(), fator.denominador(), fator.formula(),
                "Controles " + VERSAO + " | release " + release + " | ambiente " + ambiente,
                fator.estatisticas(), fator.avisos(), fator.versaoCalculo());
    }

    private static Registro maisRecente(Registro a, Registro b) {
        int ordem = a.verificadaEm().compareTo(b.verificadaEm());
        if (ordem != 0) return ordem > 0 ? a : b;
        if (a.equals(b)) return a;
        return new Registro(a.empresaId(), a.departamentoId(), a.release(), a.ambiente(),
                a.versaoCatalogo(), a.controle(), Situacao.NAO_VERIFICADO, null, null, a.verificadaEm());
    }

    private static boolean vazio(String valor) {
        return valor == null || valor.isBlank();
    }

    public record Registro(Long empresaId, Long departamentoId, String release, String ambiente,
                           String versaoCatalogo, Controle controle, Situacao situacao,
                           String referencia, String responsavel, Instant verificadaEm) {}
}
