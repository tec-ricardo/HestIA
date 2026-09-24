package br.com.hestia.gamificacao.service;

import br.com.hestia.gamificacao.dto.*;
import br.com.hestia.gamificacao.model.*;
import br.com.hestia.gamificacao.repository.*;
import br.com.hestia.usuario.model.Usuario;
import br.com.hestia.usuario.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
public class GamificacaoService {
    private final ProgressoUsuarioRepository progressoRepository;
    private final MovimentoExperienciaRepository movimentoRepository;
    private final MissaoRepository missaoRepository;
    private final ProgressoMissaoRepository progressoMissaoRepository;
    private final UsuarioRepository usuarioRepository;

    public GamificacaoService(ProgressoUsuarioRepository progressoRepository,
                              MovimentoExperienciaRepository movimentoRepository,
                              MissaoRepository missaoRepository,
                              ProgressoMissaoRepository progressoMissaoRepository,
                              UsuarioRepository usuarioRepository) {
        this.progressoRepository = progressoRepository; this.movimentoRepository = movimentoRepository;
        this.missaoRepository = missaoRepository; this.progressoMissaoRepository = progressoMissaoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional
    public PasseHestIAResponseDTO registrarAcao(RegistroAcaoDTO dto) {
        Usuario usuario = usuarioRepository.findById(dto.usuarioId())
                .orElseThrow(() -> new NoSuchElementException("Usuário não encontrado"));
        if (!movimentoRepository.existsByUsuarioIdAndReferencia(dto.usuarioId(), dto.referencia())) {
            ProgressoUsuario progresso = progressoRepository.findByUsuarioId(dto.usuarioId())
                    .orElseGet(() -> new ProgressoUsuario(usuario));
            concederXp(usuario, progresso, dto.acao(), dto.acao().getPontos(), dto.referencia());
            atualizarMissoes(usuario, progresso, dto.acao());
            progressoRepository.save(progresso);
        }
        return passe(dto.usuarioId());
    }

    @Transactional(readOnly = true)
    public PasseHestIAResponseDTO passe(Long usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new NoSuchElementException("Usuário não encontrado"));
        ProgressoUsuario progresso = progressoRepository.findByUsuarioId(usuarioId)
                .orElseGet(() -> new ProgressoUsuario(usuario));
        Map<Long, ProgressoMissao> existentes = new HashMap<>();
        progressoMissaoRepository.findByUsuarioId(usuarioId).forEach(p -> existentes.put(p.getMissao().getId(), p));
        List<PasseHestIAResponseDTO.MissaoDTO> missoes = missaoRepository.findByAtivaTrue().stream().map(m -> {
            ProgressoMissao p = existentes.get(m.getId());
            return new PasseHestIAResponseDTO.MissaoDTO(m.getCodigo(), m.getTitulo(), m.getDescricao(),
                    p == null ? 0 : p.getProgresso(), m.getMetaQuantidade(), m.getRecompensaXp(),
                    p != null && p.getConcluidaEm() != null);
        }).toList();
        NivelExperiencia proximo = progresso.getNivel().proximo();
        int percentual = proximo == null ? 100 : Math.min(100, (int) Math.round(100.0 *
                (progresso.getXpTotal() - progresso.getNivel().getXpMinimo()) /
                (proximo.getXpMinimo() - progresso.getNivel().getXpMinimo())));
        return new PasseHestIAResponseDTO(usuarioId, usuario.getNome(), progresso.getXpTotal(), progresso.getNivel(),
                proximo == null ? null : proximo.getXpMinimo(), percentual, missoes);
    }

    private void atualizarMissoes(Usuario usuario, ProgressoUsuario progresso, AcaoExperiencia acao) {
        for (Missao missao : missaoRepository.findByAtivaTrueAndAcaoAlvo(acao)) {
            ProgressoMissao progressoMissao = progressoMissaoRepository
                    .findByUsuarioIdAndMissaoId(usuario.getId(), missao.getId())
                    .orElseGet(() -> new ProgressoMissao(usuario, missao));
            if (progressoMissao.incrementar() && !progressoMissao.isRecompensaConcedida()) {
                concederXp(usuario, progresso, AcaoExperiencia.MISSAO_CONCLUIDA, missao.getRecompensaXp(),
                        "MISSAO:" + missao.getCodigo());
                progressoMissao.marcarRecompensaConcedida();
            }
            progressoMissaoRepository.save(progressoMissao);
        }
    }

    private void concederXp(Usuario usuario, ProgressoUsuario progresso, AcaoExperiencia acao,
                            int pontos, String referencia) {
        progresso.adicionarXp(pontos);
        movimentoRepository.save(new MovimentoExperiencia(usuario, acao, pontos, referencia));
    }
}
