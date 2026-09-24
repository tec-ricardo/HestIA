package br.com.hestia.gamificacao.repository;
import br.com.hestia.gamificacao.model.MovimentoExperiencia;
import org.springframework.data.jpa.repository.JpaRepository;
public interface MovimentoExperienciaRepository extends JpaRepository<MovimentoExperiencia, Long> { boolean existsByUsuarioIdAndReferencia(Long usuarioId, String referencia); }
