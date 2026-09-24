package br.com.hestia.gamificacao.repository;
import br.com.hestia.gamificacao.model.ProgressoUsuario;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface ProgressoUsuarioRepository extends JpaRepository<ProgressoUsuario, Long> { Optional<ProgressoUsuario> findByUsuarioId(Long usuarioId); }
