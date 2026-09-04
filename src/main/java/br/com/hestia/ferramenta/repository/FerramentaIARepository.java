package br.com.hestia.ferramenta.repository;

import br.com.hestia.ferramenta.model.FerramentaIA;
import br.com.hestia.ferramenta.model.StatusFerramentaIA;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FerramentaIARepository extends JpaRepository<FerramentaIA, Long> {
    List<FerramentaIA> findByEmpresaId(Long empresaId);
    List<FerramentaIA> findByEmpresaIdAndStatus(Long empresaId, StatusFerramentaIA status);
    boolean existsByEmpresaIdAndNomeIgnoreCaseAndFornecedorIgnoreCase(
            Long empresaId, String nome, String fornecedor
    );
}
