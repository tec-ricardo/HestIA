package br.com.hestia.conformidade.dto;

import br.com.hestia.politica.dto.PoliticaUsoVersaoDTO;
import br.com.hestia.registro.dto.RegistroUsoIAResponseDTO;

public record TratamentoDetalheDTO(TratamentoResumoDTO tratamento,
        RegistroUsoIAResponseDTO ocorrencia, AvaliacaoConformidadeResponseDTO avaliacao,
        PoliticaUsoVersaoDTO politicaAplicada) {}
