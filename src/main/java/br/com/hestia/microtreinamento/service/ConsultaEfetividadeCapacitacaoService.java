package br.com.hestia.microtreinamento.service;

import br.com.hestia.microtreinamento.model.TipoErro;
import br.com.hestia.usuario.model.Usuario;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ConsultaEfetividadeCapacitacaoService {

    private final RecorrenciaCapacitacaoService acompanhamento;
    private final FonteRecorrenciaConformidade fonte;
    private final RecorrenciaErroService calculador;
    private final EfetividadeCapacitacaoService indicador;

    public ConsultaEfetividadeCapacitacaoService(
            RecorrenciaCapacitacaoService acompanhamento,
            FonteRecorrenciaConformidade fonte,
            RecorrenciaErroService calculador,
            EfetividadeCapacitacaoService indicador
    ) {
        this.acompanhamento = acompanhamento;
        this.fonte = fonte;
        this.calculador = calculador;
        this.indicador = indicador;
    }

    @Transactional(
            readOnly = true,
            isolation = Isolation.REPEATABLE_READ
    )
    public Resultado consultar(
            Usuario autenticado,
            Long conclusaoId,
            TipoErro tipoErro
    ) {
        LocalDateTime observadoAte =
                LocalDateTime.now().withNano(0);

        var autorizado = acompanhamento.consultar(
                autenticado,
                conclusaoId,
                tipoErro,
                observadoAte,
                List.of()
        );

        var referencia = autorizado.recorrencia();

        var leitura = fonte.consultar(
                referencia.empresaId(),
                referencia.usuarioId(),
                referencia.tipoErro(),
                referencia.dataConclusao(),
                observadoAte
        );

        var recorrencia = calculador.calcular(
                referencia.empresaId(),
                referencia.usuarioId(),
                referencia.tipoErro(),
                referencia.dataConclusao(),
                observadoAte,
                leitura.ocorrencias()
        );

        var atualizado = new RecorrenciaCapacitacaoService.Resultado(
                autorizado.conclusaoId(),
                autorizado.microtreinamentoId(),
                autorizado.tituloMicrotreinamento(),
                recorrencia
        );

        String limitacao = leitura.tipoSuportado()
                ? "Cobertura restrita aos registros de uso cadastrados "
                + "no HestIA. A comparação não comprova relação "
                + "causal entre capacitação e mudança de comportamento."
                : "O checklist atual não permite confirmar "
                + "individualmente ocorrências deste tipo de erro.";

        return new Resultado(
                indicador.avaliar(
                        atualizado,
                        leitura.coberturaConfirmada()
                ),
                new Cobertura(
                        "AVALIACOES_DE_CONFORMIDADE_DOS_REGISTROS_CADASTRADOS",
                        leitura.tipoSuportado(),
                        leitura.coberturaConfirmada(),
                        leitura.antes(),
                        leitura.depois(),
                        limitacao
                )
        );
    }

    public record Resultado(
            EfetividadeCapacitacaoService.Resultado indicador,
            Cobertura cobertura
    ) {
    }

    public record Cobertura(
            String origem,
            boolean tipoSuportado,
            boolean confirmada,
            FonteRecorrenciaConformidade.Janela antes,
            FonteRecorrenciaConformidade.Janela depois,
            String limitacao
    ) {
    }
}