package br.com.hestia.conformidade.service;

import br.com.hestia.politica.repository.PoliticaUsoVersaoRepository;
import br.com.hestia.autenticacao.contexto.EmpresaAtual;
import br.com.hestia.conformidade.dto.FundamentosConformidade;
import br.com.hestia.politica.dto.PoliticaUsoVersaoDTO;
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
    @org.springframework.beans.factory.annotation.Autowired(required=false)
    private org.springframework.context.ApplicationEventPublisher eventosCapacitacao;
    private final AvaliacaoConformidadeRepository avaliacaoRepository;
    private final RegistroUsoIARepository registroRepository;
    private final PoliticaUsoRepository politicaRepository;
    private final PoliticaUsoVersaoRepository versoes;
    private final EmpresaAtual empresaAtual;
    public static final String REGRA_CODIGO = "CONFORMIDADE_CRITERIOS";
    public static final String REGRA_VERSAO = "1.0";

    public AvaliacaoConformidadeService(AvaliacaoConformidadeRepository avaliacaoRepository,
                                        RegistroUsoIARepository registroRepository,
                                        PoliticaUsoRepository politicaRepository,
            PoliticaUsoVersaoRepository versoes,
            EmpresaAtual empresaAtual) {
        this.avaliacaoRepository = avaliacaoRepository;
        this.registroRepository = registroRepository;
        this.politicaRepository = politicaRepository;
        this.versoes = versoes;
        this.empresaAtual = empresaAtual;
    }

    @Transactional
    public AvaliacaoConformidadeResponseDTO avaliar(AvaliacaoConformidadeDTO dto) {
        if (avaliacaoRepository.existsByRegistroUsoId(dto.registroUsoId())) {
            throw new IllegalArgumentException("O registro de uso já foi avaliado");
        }
        RegistroUsoIA registro = registroRepository.findById(dto.registroUsoId())
                .orElseThrow(() -> new NoSuchElementException("Registro de uso não encontrado"));
        PoliticaUso politica = politicaRepository.buscarBloqueada(dto.politicaUsoId())
                .orElseThrow(() -> new NoSuchElementException("Política de uso não encontrada"));
        empresaAtual.validar(registro.getUsuario().getEmpresa().getId());
        empresaAtual.validar(politica.getEmpresa().getId());
        if (politica.isExcluida()) throw new IllegalArgumentException("Política excluída não pode fundamentar nova avaliação.");
        if (!registro.getUsuario().getEmpresa().getId().equals(politica.getEmpresa().getId())) {
            throw new IllegalArgumentException("Registro e política devem pertencer à mesma empresa");
        }

        Set<CriterioConformidade> atendidos = dto.criteriosConfirmados().isEmpty()
                ? EnumSet.noneOf(CriterioConformidade.class)
                : EnumSet.copyOf(dto.criteriosConfirmados());
        aplicarCriteriosAutomaticos(registro, politica, atendidos);
        var avaliacao = new AvaliacaoConformidade(
                registro, politica, calcularStatus(atendidos), atendidos, dto.justificativa());
        var versao = versoes.findByPoliticaUsoIdAndRevisao(politica.getId(), politica.getRevisao())
                .orElseThrow(() -> new IllegalStateException("Revisão da política não registrada."));
        avaliacao.preservarFundamentos(versao, REGRA_CODIGO, REGRA_VERSAO,
                FundamentosConformidade.from(
                        avaliacao.getStatus(), avaliacao.getCriteriosAtendidos()));
        var salva=avaliacaoRepository.save(avaliacao);
        if(eventosCapacitacao!=null)eventosCapacitacao.publishEvent(new br.com.hestia.trilha.service.MotorTrilhasService.ConformidadeRegistrada(registro.getUsuario().getId(),registro.getUsuario().getEmpresa().getId()));
        return AvaliacaoConformidadeResponseDTO.from(salva);
    }

    @Transactional(readOnly = true)
    public AvaliacaoConformidadeResponseDTO buscar(Long id) {
        return AvaliacaoConformidadeResponseDTO.from(obterAvaliacao(id));
    }

    @Transactional(readOnly = true)
    public PoliticaUsoVersaoDTO buscarPoliticaHistorica(Long id) {
        var v = obterAvaliacao(id).getPoliticaVersao();
        if (v == null) throw new NoSuchElementException("Avaliação legada sem política histórica preservada.");
        return PoliticaUsoVersaoDTO.from(v);
    }

    private AvaliacaoConformidade obterAvaliacao(Long id) {
        var a = avaliacaoRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Avaliação de conformidade não encontrada"));
        empresaAtual.validar(a.getRegistroUso().getUsuario().getEmpresa().getId());
        empresaAtual.validar(a.getPoliticaUso().getEmpresa().getId());
        return a;
    }

    private void aplicarCriteriosAutomaticos(RegistroUsoIA registro, PoliticaUso politica,
                                              Set<CriterioConformidade> atendidos) {
        // Estes criterios dependem dos dados do sistema, nao da declaracao do solicitante.
        atendidos.removeAll(EnumSet.of(CriterioConformidade.REGISTRO_DE_USO,
                CriterioConformidade.FINALIDADE_ADEQUADA, CriterioConformidade.POLITICA_INTERNA,
                CriterioConformidade.IA_AUTORIZADA));
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
