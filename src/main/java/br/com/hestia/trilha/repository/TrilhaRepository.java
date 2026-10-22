package br.com.hestia.trilha.repository;
import br.com.hestia.trilha.model.Trilha;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface TrilhaRepository extends JpaRepository<Trilha,Long> {
    List<Trilha> findByEmpresaIdOrderByIdAsc(Long empresaId);
}
