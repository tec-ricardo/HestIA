package br.com.hestia.gamificacao.repository;
import br.com.hestia.gamificacao.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface MissaoRepository extends JpaRepository<Missao, Long> { List<Missao> findByAtivaTrue(); List<Missao> findByAtivaTrueAndAcaoAlvo(AcaoExperiencia acao); }
