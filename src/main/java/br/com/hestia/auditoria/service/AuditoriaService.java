package br.com.hestia.auditoria.service;

import br.com.hestia.auditoria.dto.LogAuditoriaResponseDTO;
import br.com.hestia.auditoria.model.LogAuditoria;
import br.com.hestia.auditoria.repository.LogAuditoriaRepository;
import br.com.hestia.usuario.model.Usuario;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class AuditoriaService {
    private final LogAuditoriaRepository repository;
    public AuditoriaService(LogAuditoriaRepository repository) { this.repository = repository; }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrar(Usuario usuario, String metodo, String recurso, int status, String ip) {
        repository.save(new LogAuditoria(usuario, metodo, recurso, acao(metodo), status, ip));
    }

    @Transactional(readOnly = true)
    public List<LogAuditoriaResponseDTO> listar(Long empresaId, Long usuarioId, String acao,
                                                LocalDateTime inicio, LocalDateTime fim) {
        Specification<LogAuditoria> filtro = (root, query, cb) -> {
            List<Predicate> itens = new ArrayList<>();
            if (empresaId != null) itens.add(cb.equal(root.get("empresa").get("id"), empresaId));
            if (usuarioId != null) itens.add(cb.equal(root.get("usuario").get("id"), usuarioId));
            if (acao != null && !acao.isBlank()) itens.add(cb.equal(root.get("acao"), acao.toUpperCase()));
            if (inicio != null) itens.add(cb.greaterThanOrEqualTo(root.get("dataHora"), inicio));
            if (fim != null) itens.add(cb.lessThanOrEqualTo(root.get("dataHora"), fim));
            query.orderBy(cb.desc(root.get("dataHora")));
            return cb.and(itens.toArray(Predicate[]::new));
        };
        return repository.findAll(filtro).stream().map(LogAuditoriaResponseDTO::from).toList();
    }

    private String acao(String metodo) {
        return switch (metodo) {
            case "POST" -> "CRIACAO"; case "PUT", "PATCH" -> "ALTERACAO";
            case "DELETE" -> "EXCLUSAO"; default -> "CONSULTA";
        };
    }
}
