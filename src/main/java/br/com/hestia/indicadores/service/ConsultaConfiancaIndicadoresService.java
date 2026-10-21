package br.com.hestia.indicadores.service;

import br.com.hestia.indicadores.dto.*;
import br.com.hestia.reutilizavel.service.AcessoRespostaService;
import br.com.hestia.usuario.model.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.math.*;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;

@Service
public class ConsultaConfiancaIndicadoresService {
    public enum Visao { PESSOAL, GERENCIAL }
    private final JdbcTemplate jdbc;
    private final AcessoRespostaService acesso;
    private final ComposicaoMaturidadeService composicao;
    private final RegraConfiancaService regra;

    public ConsultaConfiancaIndicadoresService(JdbcTemplate jdbc, AcessoRespostaService acesso,
            ComposicaoMaturidadeService composicao, RegraConfiancaService regra) {
        this.jdbc = jdbc; this.acesso = acesso; this.composicao = composicao; this.regra = regra;
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public IndicadoresConfiancaDTO consultar(Usuario usuario, LocalDate inicio, LocalDate fim,
            Visao visao, String versao) {
        Long empresa = acesso.validarEmpresa(usuario, null);
        LocalDateTime agora = LocalDateTime.now();
        if (inicio == null || fim == null || inicio.isAfter(fim) || fim.isAfter(agora.toLocalDate())
                || java.time.temporal.ChronoUnit.DAYS.between(inicio, fim) > 3660)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Informe início e fim válidos, sem datas futuras e com intervalo máximo de dez anos.");
        if (versao != null && !RegraConfiancaService.VERSAO.equals(versao))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Versão de confiança não encontrada.");
        if (visao == null) visao = Visao.PESSOAL;
        Long usuarioId = visao == Visao.PESSOAL ? usuario.getId() : null;
        Long departamento = null;
        if (visao == Visao.GERENCIAL) {
            if (usuario.getPerfil() == PerfilUsuario.FUNCIONARIO)
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Visão gerencial não autorizada.");
            if (usuario.getPerfil() == PerfilUsuario.GESTOR) {
                if (usuario.getDepartamento() == null
                        || !empresa.equals(usuario.getDepartamento().getEmpresa().getId()))
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Departamento inválido.");
                departamento = usuario.getDepartamento().getId();
            }
        }

        // Uma linha por registro de uso; os filtros são aplicados antes da agregação.
        // Avaliações legadas, pendentes e vínculos inconsistentes reduzem a evidência.
        String escopo = usuarioId != null ? " AND u.id = ?" :
                departamento != null ? " AND u.departamento_id = ?" : "";
        var parametros = new ArrayList<Object>();
        parametros.add(Timestamp.valueOf(agora));
        parametros.add(empresa);
        parametros.add(Timestamp.valueOf(inicio.atStartOfDay()));
        parametros.add(Timestamp.valueOf(fim.plusDays(1).atStartOfDay()));
        if (usuarioId != null) parametros.add(usuarioId);
        else if (departamento != null) parametros.add(departamento);
        String sql = """
                SELECT COUNT(*) AS universo,
                       COALESCE(SUM(CASE WHEN valida = 1 THEN 1 ELSE 0 END), 0) AS cobertos,
                       COALESCE(SUM(CASE WHEN valida = 1 AND status IN ('CONFORME','NAO_CONFORME')
                                         THEN 1 ELSE 0 END), 0) AS completos,
                       COALESCE(SUM(CASE WHEN valida = 1 AND status = 'CONFORME'
                                         THEN 1 ELSE 0 END), 0) AS conformes
                FROM (
                    SELECT COALESCE(t.resultado_vigente, a.status) AS status,
                           CASE WHEN v.empresa_id = u.empresa_id
                                 AND p.empresa_id = u.empresa_id
                                 AND f.empresa_id = u.empresa_id
                                 AND v.politica_uso_id = a.politica_uso_id
                                 AND a.regra_codigo IS NOT NULL AND TRIM(a.regra_codigo) <> ''
                                 AND a.regra_versao IS NOT NULL AND TRIM(a.regra_versao) <> ''
                                 AND a.motivo_preservado IS NOT NULL AND TRIM(a.motivo_preservado) <> ''
                                 AND a.regra_preservada IS NOT NULL AND TRIM(a.regra_preservada) <> ''
                                 AND a.data_avaliacao >= r.data_hora
                                 AND a.data_avaliacao <= ?
                                THEN 1 ELSE 0 END AS valida
                    FROM registros_uso_ia r
                    JOIN usuarios u ON u.id = r.usuario_id
                    JOIN ferramentas_ia f ON f.id = r.ferramenta_id
                    LEFT JOIN avaliacoes_conformidade a ON a.registro_uso_id = r.id
                    LEFT JOIN tratamentos_nao_conformidade t ON t.avaliacao_id = a.id AND t.empresa_id = u.empresa_id
                    LEFT JOIN politicas_uso p ON p.id = a.politica_uso_id
                    LEFT JOIN politica_uso_versoes v ON v.id = a.politica_versao_id
                    WHERE u.empresa_id = ? AND r.data_hora >= ? AND r.data_hora < ?
                """ + escopo + ") evidencias";
        long[] contagens = jdbc.queryForObject(sql, (rs, linha) -> new long[]{
                rs.getLong("universo"), rs.getLong("cobertos"), rs.getLong("completos"), rs.getLong("conformes")
        }, parametros.toArray());
        long universo = contagens[0], cobertos = contagens[1], completos = contagens[2], conformes = contagens[3];
        BigDecimal nota = completos == 0 ? null : BigDecimal.valueOf(conformes)
                .multiply(BigDecimal.valueOf(100)).divide(BigDecimal.valueOf(completos), 2, RoundingMode.HALF_UP);
        var fatores = new ArrayList<IndicadoresConfiancaDTO.Indicador>();
        var estrutura = composicao.consultar(inicio, fim, null);
        for (var fator : estrutura.fatores()) {
            boolean disponivel = "usoResponsavel".equals(fator.codigo());
            fatores.add(new IndicadoresConfiancaDTO.Indicador(fator.codigo(), fator.nome(), fator.peso(),
                    disponivel ? nota : null, "PONTOS_0_100",
                    disponivel ? "REGISTROS_AVALIACOES_E_REVISOES_OPERACIONAIS" : "CALCULADORA_PENDENTE",
                    disponivel ? "100 * conformes_vigentes_com_fundamentos / conclusivos_vigentes_com_fundamentos" : null,
                    disponivel ? conformes : null, disponivel ? completos : null,
                    disponivel ? regra.avaliar(nota, universo, cobertos, completos, completos, false)
                            : regra.indisponivel("Calculadora e fonte validada desta dimensão ainda não disponíveis.")));
        }
        BigDecimal peso = fatores.stream().filter(this::elegivel).map(IndicadoresConfiancaDTO.Indicador::peso)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal soma = fatores.stream().filter(this::elegivel).map(i -> i.peso().multiply(i.valor()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal parcial = peso.signum() == 0 ? null : soma.divide(peso, 2, RoundingMode.HALF_UP);
        return new IndicadoresConfiancaDTO(empresa, visao.name(), usuarioId, departamento,
                new PeriodoReferenciaDTO(inicio, fim), agora, "OPERACIONAL", estrutura.versaoRegra(),
                RegraConfiancaService.VERSAO, peso.compareTo(BigDecimal.ONE) == 0 ? parcial : null,
                parcial, peso, fatores, List.of(
                    "Cobertura restrita aos registros cadastrados no HestIA; não mede usos externos não registrados.",
                    "Confiável significa atender à convenção de cobertura, completude e amostra; não é garantia estatística.",
                    "Dados sintéticos de staging não são misturados com usuários operacionais.",
                    "Índice global indisponível enquanto faltarem dimensões elegíveis; índice parcial é exploratório.",
                    "Timestamps seguem a semântica local da aplicação; a base não declara fuso horário."));
    }
    private boolean elegivel(IndicadoresConfiancaDTO.Indicador i) {
        return i.valor() != null && "CONFIAVEL".equals(i.confianca().classificacao());
    }
}
