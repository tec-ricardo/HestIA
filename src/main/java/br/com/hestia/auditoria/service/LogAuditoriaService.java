package br.com.hestia.auditoria.service;

import br.com.hestia.auditoria.dto.LogAuditoriaCriacaoDTO;
import br.com.hestia.auditoria.model.LogAuditoria;
import br.com.hestia.auditoria.model.ResultadoAuditoria;
import br.com.hestia.auditoria.model.TipoAcaoAuditoria;
import br.com.hestia.auditoria.repository.LogAuditoriaRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;

@Service
public class LogAuditoriaService {

    private final LogAuditoriaRepository logAuditoriaRepository;

    public LogAuditoriaService(
            LogAuditoriaRepository logAuditoriaRepository
    ) {
        this.logAuditoriaRepository = logAuditoriaRepository;
    }

    public LogAuditoria registrar(
            LogAuditoriaCriacaoDTO dto
    ) {

        LogAuditoria log = new LogAuditoria(
                dto.usuarioId(),
                dto.acao(),
                dto.entidade(),
                dto.entidadeId(),
                dto.descricao(),
                dto.resultado(),
                dto.dadosAnteriores(),
                dto.dadosNovos()
        );

        return logAuditoriaRepository.save(log);
    }

    public LogAuditoria registrar(
            Long usuarioId,
            TipoAcaoAuditoria acao,
            String entidade,
            Long entidadeId,
            String descricao,
            ResultadoAuditoria resultado,
            String dadosAnteriores,
            String dadosNovos
    ) {

        LogAuditoria log = new LogAuditoria(
                usuarioId,
                acao,
                entidade,
                entidadeId,
                descricao,
                resultado,
                dadosAnteriores,
                dadosNovos
        );

        return logAuditoriaRepository.save(log);
    }

    public List<LogAuditoria> listarTodos() {
        return logAuditoriaRepository
                .findAllByOrderByDataHoraDesc();
    }

    public LogAuditoria buscarPorId(Long id) {
        return logAuditoriaRepository.findById(id)
                .orElseThrow(() ->
                        new NoSuchElementException(
                                "Log de auditoria não encontrado."
                        )
                );
    }

    public List<LogAuditoria> buscarPorUsuario(
            Long usuarioId
    ) {
        return logAuditoriaRepository
                .findByUsuarioIdOrderByDataHoraDesc(
                        usuarioId
                );
    }

    public List<LogAuditoria> buscarPorEntidade(
            String entidade
    ) {
        return logAuditoriaRepository
                .findByEntidadeIgnoreCaseOrderByDataHoraDesc(
                        entidade
                );
    }

    public List<LogAuditoria> buscarPorAcao(
            TipoAcaoAuditoria acao
    ) {
        return logAuditoriaRepository
                .findByAcaoOrderByDataHoraDesc(
                        acao
                );
    }
}