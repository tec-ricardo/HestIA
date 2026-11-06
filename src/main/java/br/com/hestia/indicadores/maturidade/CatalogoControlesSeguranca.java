package br.com.hestia.indicadores.maturidade;

import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;

/** Catálogo aprovado na SCRUM-539; evidências são fornecidas pelo fluxo operacional. */
public final class CatalogoControlesSeguranca {
    public static final String VERSAO = "seguranca-1.0";
    public enum Controle {
        SEG01_ACESSO_POR_PERFIL, SEG02_ISOLAMENTO, SEG03_REVOGACAO,
        SEG04_FONTES_RAG, SEG05_PROTECAO_CREDENCIAIS,
        SEG06_AUDITORIA, SEG07_FALHAS_E_IDEMPOTENCIA, SEG08_RESTAURACAO
    }
    public enum Situacao { ATENDIDO, NAO_ATENDIDO, NAO_VERIFICADO }
    public record Evidencia(Controle controle, Situacao situacao, String referencia,
            String versaoRelease, Instant verificadaEm) {}
    private CatalogoControlesSeguranca() {}

    public static FatorCalculado avaliar(List<Evidencia> evidencias, String release, Instant corte) {
        Objects.requireNonNull(evidencias); Objects.requireNonNull(corte);
        if (release == null || release.isBlank()) throw new IllegalArgumentException("Informe a versão avaliada");
        var vistos = EnumSet.noneOf(Controle.class);
        long atendidos = 0, avaliados = 0;
        for (var e : evidencias) {
            if (e == null || e.controle() == null || e.situacao() == null || !vistos.add(e.controle()))
                throw new IllegalArgumentException("Controle inválido ou duplicado");
            if (e.situacao() == Situacao.NAO_VERIFICADO) continue;
            // Sem prova desta versão e deste corte, o controle permanece desconhecido.
            if (!release.equals(e.versaoRelease()) || e.referencia() == null || e.referencia().isBlank()
                    || e.verificadaEm() == null || e.verificadaEm().isAfter(corte)) continue;
            avaliados++;
            if (e.situacao() == Situacao.ATENDIDO) atendidos++;
        }
        return new CalculadorasMaturidade().seguranca(atendidos, avaliados, Controle.values().length);
    }
}
