package br.com.hestia.indicadores.service;

import br.com.hestia.indicadores.dto.ConsultaCoberturaDTO;
import br.com.hestia.reutilizavel.service.AcessoRespostaService;
import br.com.hestia.usuario.model.Usuario;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Map;

@Service
public class ConsultaCoberturaService {

    private final ComposicaoMaturidadeService composicao;
    private final CoberturaMaturidadeService calculador;
    private final AcessoRespostaService acesso;

    public ConsultaCoberturaService(
            ComposicaoMaturidadeService composicao,
            CoberturaMaturidadeService calculador,
            AcessoRespostaService acesso
    ) {
        this.composicao = composicao;
        this.calculador = calculador;
        this.acesso = acesso;
    }

    public ConsultaCoberturaDTO consultar(
            Usuario usuario,
            LocalDate inicio,
            LocalDate fim,
            String versao
    ) {
        Long empresaId = acesso.validarEmpresa(usuario, null);
        var regra = composicao.consultar(inicio, fim, versao);

        var cobertura = calculador.calcular(regra, Map.of());

        return new ConsultaCoberturaDTO(
                empresaId,
                "SEM_FONTES_DE_NOTAS_IMPLEMENTADAS",
                cobertura
        );
    }
}