package br.com.hestia.ferramenta.service;

import br.com.hestia.empresa.repository.EmpresaRepository;
import br.com.hestia.ferramenta.dto.FerramentaIACriacaoDTO;
import br.com.hestia.ferramenta.model.FerramentaIA;
import br.com.hestia.ferramenta.model.NivelRiscoIA;
import br.com.hestia.ferramenta.repository.FerramentaIARepository;
import org.springframework.stereotype.Service;

import br.com.hestia.ferramenta.model.StatusFerramentaIA;
import br.com.hestia.usuario.model.PerfilUsuario;
import br.com.hestia.usuario.model.Usuario;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.NoSuchElementException;

@Service
public class FerramentaIAService {

    private final FerramentaIARepository ferramentaRepository;
    private final EmpresaRepository empresaRepository;
    private final JdbcTemplate jdbc;

    public FerramentaIAService(FerramentaIARepository ferramentaRepository,
                               EmpresaRepository empresaRepository, JdbcTemplate jdbc) {
        this.ferramentaRepository = ferramentaRepository;
        this.empresaRepository = empresaRepository;
        this.jdbc = jdbc;
    }

    public FerramentaIA criar(FerramentaIACriacaoDTO dto) {
        var empresa = empresaRepository.findById(dto.empresaId())
                .orElseThrow(() -> new NoSuchElementException("Empresa não encontrada."));

        if (ferramentaRepository.existsByEmpresaIdAndNomeIgnoreCaseAndFornecedorIgnoreCase(
                dto.empresaId(), dto.nome(), dto.fornecedor())) {
            throw new IllegalArgumentException(
                    "Já existe uma ferramenta com esse nome e fornecedor nesta empresa.");
        }

        return ferramentaRepository.save(new FerramentaIA(
                dto.nome(), dto.fornecedor(), dto.descricao(), dto.tipo(),
                dto.finalidadeUso(), dto.urlAcesso(), dto.trataDadosPessoais(), empresa));
    }

    public List<FerramentaIA> listar(Long empresaId) {
        return empresaId == null
                ? ferramentaRepository.findAll()
                : ferramentaRepository.findByEmpresaId(empresaId);
    }

    public List<FerramentaIA> listarPorNivelRisco(NivelRiscoIA nivelRisco) {
        return ferramentaRepository.findByNivelRisco(nivelRisco);
    }

    public FerramentaIA buscar(Long id) {
        return ferramentaRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Ferramenta de IA não encontrada."));
    }

    @Transactional
    public void registrarAutorizacao(Usuario autor, Long id, StatusFerramentaIA status, String motivo) {
        if (autor.getPerfil() != PerfilUsuario.ADMIN || !Boolean.TRUE.equals(autor.getAtivo())
                || autor.getEmpresa() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Apenas administradores da empresa podem decidir autorizações.");
        }
        if ((status != StatusFerramentaIA.APROVADA && status != StatusFerramentaIA.DESCONTINUADA)
                || motivo == null || motivo.isBlank() || motivo.length() > 1000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe aprovação ou retirada e um motivo válido.");
        }

        Long empresaId = autor.getEmpresa().getId();
        var ferramenta = jdbc.queryForList(
                "SELECT id FROM ferramentas_ia WHERE id = ? AND empresa_id = ? FOR UPDATE",
                Long.class, id, empresaId);
        if (ferramenta.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Ferramenta indisponível.");
        }

        var aberta = jdbc.queryForList("""
                SELECT status, inicio_vigencia FROM autorizacoes_ferramentas_ia
                WHERE empresa_id = ? AND ferramenta_id = ? AND fim_vigencia IS NULL
                """, empresaId, id);
        LocalDateTime agora = LocalDateTime.now().truncatedTo(ChronoUnit.MICROS);
        if (!aberta.isEmpty()) {
            var anterior = aberta.getFirst();
            if (status.name().equals(anterior.get("status"))) {
                return;
            }
            var inicio = ((java.sql.Timestamp) anterior.get("inicio_vigencia")).toLocalDateTime();
            if (!agora.isAfter(inicio)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Aguarde para registrar outra decisão.");
            }
            jdbc.update("""
                    UPDATE autorizacoes_ferramentas_ia SET fim_vigencia = ?
                    WHERE empresa_id = ? AND ferramenta_id = ? AND fim_vigencia IS NULL
                    """, agora, empresaId, id);
        }
        jdbc.update("""
                INSERT INTO autorizacoes_ferramentas_ia
                    (empresa_id, ferramenta_id, status, inicio_vigencia, responsavel_id, motivo)
                VALUES (?, ?, ?, ?, ?, ?)
                """, empresaId, id, status.name(), agora, autor.getId(), motivo.strip());
        jdbc.update("UPDATE ferramentas_ia SET status = ?, data_atualizacao = ? WHERE id = ? AND empresa_id = ?",
                status.name(), agora, id, empresaId);
    }

    public void excluir(Long id) {
        ferramentaRepository.delete(buscar(id));
    }
}
