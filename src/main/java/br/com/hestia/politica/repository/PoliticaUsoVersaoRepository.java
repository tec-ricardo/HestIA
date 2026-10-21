package br.com.hestia.politica.repository;

import br.com.hestia.politica.model.PoliticaUsoVersao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface PoliticaUsoVersaoRepository extends JpaRepository<PoliticaUsoVersao, Long> {
    @Query("select v from PoliticaUsoVersao v where v.politicaUso.id = :politicaUsoId order by v.revisao desc")
    List<PoliticaUsoVersao> findByPoliticaUsoIdOrderByRevisaoDesc(@Param("politicaUsoId") Long politicaUsoId);

    @Query("select v from PoliticaUsoVersao v where v.politicaUso.id = :politicaUsoId and v.revisao = :revisao")
    Optional<PoliticaUsoVersao> findByPoliticaUsoIdAndRevisao(
            @Param("politicaUsoId") Long politicaUsoId, @Param("revisao") int revisao);
}
