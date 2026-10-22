package br.com.hestia.trilha.repository;
import br.com.hestia.trilha.model.ParticipacaoTrilha;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface ParticipacaoTrilhaRepository extends JpaRepository<ParticipacaoTrilha,Long> {
    Optional<ParticipacaoTrilha> findByTrilhaIdAndUsuarioId(Long trilhaId,Long usuarioId);
    List<ParticipacaoTrilha> findByUsuarioIdAndEmpresaIdOrderByIdAsc(Long usuarioId,Long empresaId);
}
