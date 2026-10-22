package br.com.hestia.indicadores.service;

import br.com.hestia.indicadores.dto.CoberturaFatorDTO;
import br.com.hestia.indicadores.dto.CoberturaMaturidadeDTO;
import br.com.hestia.indicadores.dto.ConsultaCoberturaDTO;
import br.com.hestia.indicadores.maturidade.FatorCalculado;
import br.com.hestia.indicadores.maturidade.ResultadoMaturidadeService;
import br.com.hestia.usuario.model.Usuario;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ConsultaCoberturaService {

    private final ComposicaoMaturidadeService composicao;
    private final ResultadoMaturidadeService resultados;
    private final CoberturaMaturidadeService calculador;

    public ConsultaCoberturaService(
            ComposicaoMaturidadeService composicao,
            ResultadoMaturidadeService resultados,
            CoberturaMaturidadeService calculador
    ) {
        this.composicao = composicao;
        this.resultados = resultados;
        this.calculador = calculador;
    }

    public ConsultaCoberturaDTO consultar(
            Usuario usuario, LocalDate inicio, LocalDate fim, String versao
    ) {
        return consultar(usuario, versao, new ResultadoMaturidadeService.Consulta(
                inicio, fim, ResultadoMaturidadeService.Natureza.OPERACIONAL,
                null, null, null, null
        ));
    }

    public ConsultaCoberturaDTO consultar(
            Usuario usuario,
            String versao,
            ResultadoMaturidadeService.Consulta consulta
    ) {
        var regra = composicao.consultar(consulta.inicio(), consulta.fim(), versao);
        var resultado = resultados.consultar(usuario, consulta);
        if (!regra.versaoRegra().equals(resultado.versaoPesos())) {
            throw new IllegalStateException("Versão dos pesos incompatível com a cobertura.");
        }

        var porCodigo = resultado.fatores().stream().collect(Collectors.toMap(
                FatorCalculado::codigo, Function.identity()
        ));
        if (porCodigo.size() != regra.fatores().size()) {
            throw new IllegalStateException("Resultado com composição de fatores incompatível.");
        }

        var notasElegiveis = resultado.fatores().stream()
                .filter(fator -> fator.elegivelGlobal() && fator.nota() != null)
                .collect(Collectors.toMap(FatorCalculado::codigo, FatorCalculado::nota));
        var cobertura = calculador.calcular(regra, notasElegiveis);

        var fatores = regra.fatores().stream().map(fator -> {
            var calculado = porCodigo.get(fator.codigo());
            if (calculado == null || fator.peso().compareTo(calculado.peso()) != 0) {
                throw new IllegalStateException("Fator ou peso incompatível: " + fator.codigo());
            }
            boolean disponivel = calculado.elegivelGlobal() && calculado.nota() != null;
            return new CoberturaFatorDTO(
                    fator.codigo(), fator.nome(), fator.peso(), calculado.nota(),
                    disponivel, disponivel ? null : calculado.motivo()
            );
        }).toList();

        int disponiveis = cobertura.fatoresDisponiveis();
        int total = cobertura.totalFatores();
        String situacao = disponiveis == total ? "COMPLETO"
                : disponiveis == 0 ? "SEM_FATORES_ELEGIVEIS" : "PARCIAL";

        return new ConsultaCoberturaDTO(
                resultado.empresaId(), situacao,
                new CoberturaMaturidadeDTO(
                        regra.versaoRegra(), regra.periodoReferencia(), total,
                        disponiveis, cobertura.coberturaPercentual(),
                        resultado.scoreGlobal(), fatores
                )
        );
    }
}
