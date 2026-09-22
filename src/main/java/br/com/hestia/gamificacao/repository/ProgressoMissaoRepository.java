package br.com.hestia.gamificacao.repository;
import br.com.hestia.gamificacao.model.ProgressoMissao;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
public interface ProgressoMissaoRepository extends JpaRepository<ProgressoMissao, Long> {
    Optional<ProgressoMissao> findByUsuarioIdAndMissaoId(Long usuarioId, Long missaoId);
    List<ProgressoMissao> findByUsuarioId(Long usuarioId);
}
