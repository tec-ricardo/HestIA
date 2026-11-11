package br.com.hestia.indicadores.service;

import br.com.hestia.registro.model.OrigemRegistro;
import br.com.hestia.registro.model.RegistroUsoIA;
import br.com.hestia.registro.model.StatusExecucao;
import br.com.hestia.registro.repository.RegistroUsoIARepository;
import br.com.hestia.reutilizavel.service.AcessoRespostaService;
import br.com.hestia.usuario.model.PerfilUsuario;
import br.com.hestia.usuario.model.Usuario;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class QualidadeDadosService {
    public static final String VERSAO = "elegibilidade-1.1";

    private final RegistroUsoIARepository registros;
    private final AcessoRespostaService acesso;

    public QualidadeDadosService(RegistroUsoIARepository registros, AcessoRespostaService acesso) {
        this.registros = registros;
        this.acesso = acesso;
    }

    public record Problema(Long registroId, String codigo) {}

    public record Resultado(
            String versao,
            Long empresaId,
            Long usuarioId,
            LocalDate inicio,
            LocalDate fim,
            String fonte,
            long totalRegistros,
            long registrosElegiveis,
            long registrosExcluidos,
            Double cobertura,
            int minimoRegistros,
            boolean calculavel,
            String status,
            List<Problema> problemas,
            List<Long> idsElegiveis,
            Long departamentoId
    ) {
        public Resultado comDepartamento(Long id) {
            return new Resultado(
                    versao, empresaId, usuarioId, inicio, fim, fonte, totalRegistros,
                    registrosElegiveis, registrosExcluidos, cobertura, minimoRegistros,
                    calculavel, status, problemas, idsElegiveis, id
            );
        }
    }

    @Transactional(readOnly = true)
    public Resultado consultar(Usuario u, LocalDate inicio, LocalDate fim, int minimo) {
        Long empresa = acesso.validarEmpresa(u, null);

        if (inicio == null || fim == null || inicio.isAfter(fim)
                || fim.isAfter(LocalDate.now()) || minimo < 1 || minimo > 100000) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Período e mínimo de amostra inválidos"
            );
        }

        Long usuario = u.getPerfil() == PerfilUsuario.FUNCIONARIO ? u.getId() : null;
        boolean gestor = u.getPerfil() == PerfilUsuario.GESTOR;

        if (gestor && (u.getDepartamento() == null
                || u.getDepartamento().getEmpresa() == null
                || !empresa.equals(u.getDepartamento().getEmpresa().getId()))) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Gestor sem departamento autorizado"
            );
        }

        Long departamento = gestor ? u.getDepartamento().getId() : null;
        Specification<RegistroUsoIA> filtro = (r, q, cb) -> {
            var base = cb.and(
                    cb.equal(r.get("usuario").get("empresa").get("id"), empresa),
                    cb.greaterThanOrEqualTo(r.get("dataHora"), inicio.atStartOfDay()),
                    cb.lessThan(r.get("dataHora"), fim.plusDays(1).atStartOfDay())
            );
            if (departamento != null) {
                base = cb.and(
                        base, cb.equal(r.get("usuario").get("departamento").get("id"), departamento)
                );
            }
            return usuario == null ? base
                    : cb.and(base, cb.equal(r.get("usuario").get("id"), usuario));
        };

        return avaliar(empresa, usuario, inicio, fim, minimo, registros.findAll(filtro))
                .comDepartamento(departamento);
    }

    public Resultado avaliar(
            Long empresa, Long usuario, LocalDate inicio, LocalDate fim,
            int minimo, List<RegistroUsoIA> fonte
    ) {
        if (empresa == null || minimo < 1 || fonte == null
                || inicio == null || fim == null || inicio.isAfter(fim)) {
            throw new IllegalArgumentException("Escopo e amostra obrigatórios");
        }

        List<Problema> problemas = new ArrayList<>();
        List<Long> elegiveis = new ArrayList<>();
        Map<Long, Long> frequencias = new HashMap<>();
        long total = 0;

        for (var item : fonte) {
            if (item != null && item.getId() != null && item.getUsuario() != null
                    && item.getUsuario().getEmpresa() != null
                    && empresa.equals(item.getUsuario().getEmpresa().getId())
                    && (usuario == null || usuario.equals(item.getUsuario().getId()))) {
                frequencias.merge(item.getId(), 1L, Long::sum);
            }
        }

        for (var r : fonte) {
            if (r == null) {
                problemas.add(new Problema(null, "REGISTRO_NULO"));
                continue;
            }

            if (r.getUsuario() == null || r.getUsuario().getEmpresa() == null
                    || !empresa.equals(r.getUsuario().getEmpresa().getId())
                    || (usuario != null && !usuario.equals(r.getUsuario().getId()))) {
                continue;
            }

            var dataHora = r.getDataHora();
            if (dataHora != null && (dataHora.isBefore(inicio.atStartOfDay())
                    || !dataHora.isBefore(fim.plusDays(1).atStartOfDay()))) {
                continue;
            }

            total++;
            List<String> erros = new ArrayList<>();

            if (r.getId() == null) {
                erros.add("SEM_IDENTIFICADOR");
            } else if (frequencias.getOrDefault(r.getId(), 0L) > 1) {
                erros.add("DUPLICADO");
            }

            if (dataHora == null || dataHora.isAfter(LocalDateTime.now())) {
                erros.add("DATA_INVALIDA");
            }

            var departamento = r.getUsuario().getDepartamento();
            if (departamento == null || departamento.getId() == null) {
                erros.add("DEPARTAMENTO_AUSENTE");
            } else if (departamento.getEmpresa() == null
                    || !empresa.equals(departamento.getEmpresa().getId())) {
                erros.add("DEPARTAMENTO_FORA_ESCOPO");
            }

            if (r.getFerramenta() == null || r.getFerramenta().getEmpresa() == null
                    || !empresa.equals(r.getFerramenta().getEmpresa().getId())) {
                erros.add("FERRAMENTA_FORA_ESCOPO");
            }
            if (r.getModeloIa() == null || r.getModeloIa().isBlank() || r.getFinalidade() == null) {
                erros.add("METADADOS_INCOMPLETOS");
            }
            if (r.getStatusExecucao() != StatusExecucao.SUCESSO) {
                erros.add("EXECUCAO_SEM_SUCESSO");
            }

            var entrada = r.getTokensEntrada();
            var saida = r.getTokensSaida();
            var tokensTotal = r.getTokensTotal();
            if (entrada == null || saida == null || entrada < 0 || saida < 0
                    || tokensTotal == null || tokensTotal.longValue() != (long) entrada + saida) {
                erros.add("TOKENS_INCONSISTENTES");
            }

            if (r.getCustoEstimado() != null && r.getCustoEstimado().signum() < 0) {
                erros.add("CUSTO_INVALIDO");
            }
            if (r.getTempoRespostaMs() != null && r.getTempoRespostaMs() < 0) {
                erros.add("TEMPO_INVALIDO");
            }

            if (erros.isEmpty()) {
                elegiveis.add(r.getId());
                if (r.getOrigem() == null || r.getOrigem() == OrigemRegistro.DESCONHECIDA) {
                    problemas.add(new Problema(r.getId(), "ORIGEM_NAO_IDENTIFICADA"));
                }
            } else {
                erros.forEach(erro -> problemas.add(new Problema(r.getId(), erro)));
            }
        }

        boolean calculavel = elegiveis.size() >= minimo;
        return new Resultado(
                VERSAO, empresa, usuario, inicio, fim, "registros_uso_ia",
                total, elegiveis.size(), total - elegiveis.size(),
                total == 0 ? null : 100.0 * elegiveis.size() / total,
                minimo, calculavel, calculavel ? "DADOS_DISPONIVEIS" : "DADOS_INSUFICIENTES",
                List.copyOf(problemas), List.copyOf(elegiveis), null
        );
    }

    public void exigirDadosMinimos(Resultado r) {
        if (!r.calculavel()) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Dados insuficientes: " + r.registrosElegiveis()
                            + " registros elegíveis; mínimo " + r.minimoRegistros()
            );
        }
    }
}