package br.com.hestia.conformidade.repository;

import br.com.hestia.conformidade.model.TratamentoNaoConformidade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface TratamentoNaoConformidadeRepository extends
        JpaRepository<TratamentoNaoConformidade, Long>,
        JpaSpecificationExecutor<TratamentoNaoConformidade> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select t from TratamentoNaoConformidade t where t.id = :id")
    java.util.Optional<TratamentoNaoConformidade> buscarBloqueado(
            @org.springframework.data.repository.query.Param("id") Long id);

    boolean existsByAvaliacao_Id(Long avaliacaoId);
}
