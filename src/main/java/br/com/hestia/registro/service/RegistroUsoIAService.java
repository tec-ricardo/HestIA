package br.com.hestia.registro.service;

import br.com.hestia.configuracao.repository.ConfiguracaoEmpresaRepository;
import br.com.hestia.ferramenta.model.FerramentaIA;
import br.com.hestia.ferramenta.model.StatusFerramentaIA;
import br.com.hestia.ferramenta.repository.FerramentaIARepository;
import br.com.hestia.registro.dto.EficienciaUsoDTO;
import br.com.hestia.registro.dto.RegistroUsoIACriacaoDTO;
import br.com.hestia.registro.dto.RegistroUsoIAResponseDTO;
import br.com.hestia.registro.model.FinalidadeUso;
import br.com.hestia.registro.model.RegistroUsoIA;
import br.com.hestia.registro.model.StatusExecucao;
import br.com.hestia.registro.repository.RegistroUsoIARepository;
import br.com.hestia.usuario.model.Usuario;
import br.com.hestia.usuario.repository.UsuarioRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class RegistroUsoIAService {

    private final RegistroUsoIARepository registroRepository;
    private final UsuarioRepository usuarioRepository;
    private final FerramentaIARepository ferramentaRepository;
    private final ConfiguracaoEmpresaRepository configuracaoRepository;

    public RegistroUsoIAService(RegistroUsoIARepository registroRepository,
                                UsuarioRepository usuarioRepository,
                                FerramentaIARepository ferramentaRepository,
                                ConfiguracaoEmpresaRepository configuracaoRepository) {
        this.registroRepository = registroRepository;
        this.usuarioRepository = usuarioRepository;
        this.ferramentaRepository = ferramentaRepository;
        this.configuracaoRepository = configuracaoRepository;
    }

    @Transactional
    public RegistroUsoIAResponseDTO criar(RegistroUsoIACriacaoDTO dto) {
        Usuario usuario = usuarioRepository.findById(dto.usuarioId())
                .orElseThrow(() -> new NoSuchElementException("Usuário não encontrado"));

        FerramentaIA ferramenta = ferramentaRepository.findById(dto.ferramentaId())
                .orElseThrow(() -> new NoSuchElementException("Ferramenta de IA não encontrada"));

        validarMesmoTenant(usuario, ferramenta);
        validarFerramentaDisponivel(ferramenta);

        RegistroUsoIA registro = new RegistroUsoIA(
                usuario,
                ferramenta,
                dto.modeloIa(),
                dto.dataHora(),
                dto.finalidade(),
                dto.tokensEntrada(),
                dto.tokensSaida(),
                dto.custoEstimado(),
                dto.statusExecucao(),
                dto.tempoRespostaMs()
        );

        return RegistroUsoIAResponseDTO.from(
                registroRepository.save(registro)
        );
    }

    @Transactional(readOnly = true)
    public RegistroUsoIAResponseDTO buscar(Long id) {
        return registroRepository.findById(id)
                .map(RegistroUsoIAResponseDTO::from)
                .orElseThrow(() ->
                        new NoSuchElementException(
                                "Registro de uso não encontrado"
                        )
                );
    }

    @Transactional(readOnly = true)
    public List<RegistroUsoIAResponseDTO> listar(
            Long usuarioId,
            Long ferramentaId,
            FinalidadeUso finalidade,
            LocalDateTime dataInicial,
            LocalDateTime dataFinal) {

        if (dataInicial != null
                && dataFinal != null
                && dataInicial.isAfter(dataFinal)) {

            throw new IllegalArgumentException(
                    "A data inicial não pode ser posterior à data final"
            );
        }

        Specification<RegistroUsoIA> filtros =
                (root, query, cb) -> {

                    List<Predicate> predicates =
                            new ArrayList<>();

                    if (usuarioId != null) {
                        predicates.add(
                                cb.equal(
                                        root.get("usuario").get("id"),
                                        usuarioId
                                )
                        );
                    }

                    if (ferramentaId != null) {
                        predicates.add(
                                cb.equal(
                                        root.get("ferramenta").get("id"),
                                        ferramentaId
                                )
                        );
                    }

                    if (finalidade != null) {
                        predicates.add(
                                cb.equal(
                                        root.get("finalidade"),
                                        finalidade
                                )
                        );
                    }

                    if (dataInicial != null) {
                        predicates.add(
                                cb.greaterThanOrEqualTo(
                                        root.get("dataHora"),
                                        dataInicial
                                )
                        );
                    }

                    if (dataFinal != null) {
                        predicates.add(
                                cb.lessThanOrEqualTo(
                                        root.get("dataHora"),
                                        dataFinal
                                )
                        );
                    }

                    query.orderBy(
                            cb.desc(root.get("dataHora"))
                    );

                    return cb.and(
                            predicates.toArray(
                                    Predicate[]::new
                            )
                    );
                };

        return registroRepository.findAll(filtros)
                .stream()
                .map(RegistroUsoIAResponseDTO::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<EficienciaUsoDTO> calcularEficiencia() {

        List<RegistroUsoIA> registros =
                registroRepository.findAll();

        return registros.stream()
                .collect(
                        Collectors.groupingBy(
                                registro ->
                                        registro
                                                .getUsuario()
                                                .getDepartamento()
                                                .getNome()
                        )
                )
                .entrySet()
                .stream()
                .map(entry -> {

                    String departamento =
                            entry.getKey();

                    List<RegistroUsoIA> registrosDepartamento =
                            entry.getValue();

                    long utilizacoes =
                            registrosDepartamento.size();

                    long sucessos =
                            registrosDepartamento.stream()
                                    .filter(registro ->
                                            registro.getStatusExecucao()
                                                    == StatusExecucao.SUCESSO
                                    )
                                    .count();

                    double eficiencia =
                            utilizacoes == 0
                                    ? 0
                                    : (sucessos * 100.0)
                                    / utilizacoes;

                    BigDecimal custoIA =
                            registrosDepartamento.stream()
                                    .map(
                                            RegistroUsoIA::getCustoEstimado
                                    )
                                    .filter(Objects::nonNull)
                                    .reduce(
                                            BigDecimal.ZERO,
                                            BigDecimal::add
                                    );

                    return new EficienciaUsoDTO(
                            departamento,
                            utilizacoes,
                            Math.round(
                                    eficiencia * 100.0
                            ) / 100.0,
                            custoIA
                    );

                })
                .toList();
    }

    private void validarMesmoTenant(
            Usuario usuario,
            FerramentaIA ferramenta) {

        if (!usuario.getEmpresa()
                .getId()
                .equals(
                        ferramenta
                                .getEmpresa()
                                .getId()
                )) {

            throw new IllegalArgumentException(
                    "Usuário e ferramenta devem pertencer à mesma empresa"
            );
        }
    }

    private void validarFerramentaDisponivel(
            FerramentaIA ferramenta) {

        if (ferramenta.getStatus()
                == StatusFerramentaIA.BLOQUEADA
                || ferramenta.getStatus()
                == StatusFerramentaIA.DESCONTINUADA) {

            throw new IllegalArgumentException(
                    "A ferramenta não está disponível para uso"
            );
        }

        boolean bloquearNaoAprovadas =
                configuracaoRepository
                        .findByEmpresaId(
                                ferramenta
                                        .getEmpresa()
                                        .getId()
                        )
                        .map(configuracao ->
                                configuracao
                                        .isBloquearFerramentasNaoAprovadas()
                        )
                        .orElse(true);

        if (bloquearNaoAprovadas
                && ferramenta.getStatus()
                != StatusFerramentaIA.APROVADA) {

            throw new IllegalArgumentException(
                    "A configuração da empresa permite somente ferramentas aprovadas"
            );
        }
    }
}