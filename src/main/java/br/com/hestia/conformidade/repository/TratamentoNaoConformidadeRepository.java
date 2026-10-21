package br.com.hestia.conformidade.repository;

import br.com.hestia.conformidade.model.TratamentoNaoConformidade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface TratamentoNaoConformidadeRepository extends
        JpaRepository<TratamentoNaoConformidade, Long>,
        JpaSpecificationExecutor<TratamentoNaoConformidade> {
    boolean existsByAvaliacao_Id(Long avaliacaoId);
}
