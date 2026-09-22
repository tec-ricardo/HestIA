package br.com.hestia.conformidade.service;

import br.com.hestia.conformidade.dto.AvaliacaoConformidadeDTO;
import br.com.hestia.conformidade.dto.AvaliacaoConformidadeResponseDTO;
import br.com.hestia.conformidade.model.*;
import br.com.hestia.conformidade.repository.AvaliacaoConformidadeRepository;
import br.com.hestia.ferramenta.model.StatusFerramentaIA;
import br.com.hestia.politica.model.PoliticaUso;
import br.com.hestia.politica.repository.PoliticaUsoRepository;
import br.com.hestia.registro.model.RegistroUsoIA;
import br.com.hestia.registro.repository.RegistroUsoIARepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.NoSuchElementException;
import java.util.Set;

@Service
public class AvaliacaoConformidadeService {
    private final AvaliacaoConformidadeRepository avaliacaoRepository;
    private final RegistroUsoIARepository registroRepository;
    private final PoliticaUsoRepository politicaRepository;

    public AvaliacaoConformidadeService(AvaliacaoConformidadeRepository avaliacaoRepository,
                                        RegistroUsoIARepository registroRepository,
                                        PoliticaUsoRepository politicaRepository) {
        this.avaliacaoRepository = avaliacaoRepository;
        this.registroRepository = registroRepository;
        this.politicaRepository = politicaRepository;
    }

    @Transactional
    public AvaliacaoConformidadeResponseDTO avaliar(AvaliacaoConformidadeDTO dto) {
        if (avaliacaoRepository.existsByRegistroUsoId(dto.registroUsoId())) {
            throw new IllegalArgumentException("O registro de uso já foi avaliado");
        }
        RegistroUsoIA registro = registroRepository.findById(dto.registroUsoId())
                .orElseThrow(() -> new NoSuchElementException("Registro de uso não encontrado"));
        PoliticaUso politica = politicaRepository.findById(dto.politicaUsoId())
                .orElseThrow(() -> new NoSuchElementException("Política de uso não encontrada"));
        if (!registro.getUsuario().getEmpresa().getId().equals(politica.getEmpresa().getId())) {
            throw new IllegalArgumentException("Registro e política devem pertencer à mesma empresa");
        }

        Set<CriterioConformidade> atendidos = dto.criteriosConfirmados().isEmpty()
                ? EnumSet.noneOf(CriterioConformidade.class)
                : EnumSet.copyOf(dto.criteriosConfirmados());
        aplicarCriteriosAutomaticos(registro, politica, atendidos);
        var avaliacao = new AvaliacaoConformidade(
                registro, politica, calcularStatus(atendidos), atendidos, dto.justificativa());
        return AvaliacaoConformidadeResponseDTO.from(avaliacaoRepository.save(avaliacao));
    }

    @Transactional(readOnly = true)
    public AvaliacaoConformidadeResponseDTO buscar(Long id) {
        return avaliacaoRepository.findById(id)
                .map(AvaliacaoConformidadeResponseDTO::from)
                .orElseThrow(() -> new NoSuchElementException("Avaliação de conformidade não encontrada"));
    }

    private void aplicarCriteriosAutomaticos(RegistroUsoIA registro, PoliticaUso politica,
                                              Set<CriterioConformidade> atendidos) {
        atendidos.add(CriterioConformidade.REGISTRO_DE_USO);
        if (registro.getFinalidade() != null) atendidos.add(CriterioConformidade.FINALIDADE_ADEQUADA);
        if (Boolean.TRUE.equals(politica.getAtiva())) atendidos.add(CriterioConformidade.POLITICA_INTERNA);
        if (registro.getFerramenta().getStatus() == StatusFerramentaIA.APROVADA) {
            atendidos.add(CriterioConformidade.IA_AUTORIZADA);
        }
    }

    private StatusConformidade calcularStatus(Set<CriterioConformidade> atendidos) {
        if (atendidos.containsAll(EnumSet.allOf(CriterioConformidade.class))) {
            return StatusConformidade.CONFORME;
        }
        boolean falhaCritica = !atendidos.contains(CriterioConformidade.IA_AUTORIZADA)
                || !atendidos.contains(CriterioConformidade.POLITICA_INTERNA)
                || !atendidos.contains(CriterioConformidade.LGPD);
        return falhaCritica ? StatusConformidade.NAO_CONFORME : StatusConformidade.REQUER_REVISAO;
    }
}
