package br.com.hestia.rag.repository;

import br.com.hestia.rag.model.RespostaVersao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RespostaVersaoRepository
        extends JpaRepository<RespostaVersao, Long> {

    List<RespostaVersao>
    findByRespostaIdAndRespostaEmpresaIdOrderByNumeroVersaoDesc(
            Long respostaId,
            Long empresaId
    );

    Optional<RespostaVersao>
    findTopByRespostaIdAndRespostaEmpresaIdOrderByNumeroVersaoDesc(
            Long respostaId,
            Long empresaId
    );
}