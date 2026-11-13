package br.com.hestia.indicadores.maturidade;

import br.com.hestia.reutilizavel.service.AcessoRespostaService;
import br.com.hestia.usuario.model.PerfilUsuario;
import br.com.hestia.usuario.model.Usuario;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@RestController
public class RankingDepartamentalController {
    private final JdbcTemplate jdbc;
    private final AcessoRespostaService acesso;
    private final ResultadoMaturidadeService maturidade;
    private final ClassificacaoRiscoDepartamental classificacao;

    public RankingDepartamentalController(JdbcTemplate jdbc, AcessoRespostaService acesso,
                                          ResultadoMaturidadeService maturidade, ClassificacaoRiscoDepartamental classificacao) {
        this.jdbc = jdbc;
        this.acesso = acesso;
        this.maturidade = maturidade;
        this.classificacao = classificacao;
    }

    @GetMapping("/indicadores/maturidade/ranking-departamental")
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public Resposta consultar(
            @RequestAttribute("usuarioAutenticado") Usuario usuario,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim) {
        Long empresa = acesso.validarEmpresa(usuario, null);
        boolean administrador = usuario.getPerfil() == PerfilUsuario.ADMIN;
        if (!administrador && usuario.getPerfil() != PerfilUsuario.GESTOR) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Perfil sem acesso ao ranking");
        }
        if (inicio.isAfter(fim) || fim.equals(LocalDate.MAX)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Período inválido");
        }
        Long departamento = null;
        if (!administrador) {
            var atual = usuario.getDepartamento();
            if (atual == null || atual.getId() == null || atual.getEmpresa() == null
                    || !empresa.equals(atual.getEmpresa().getId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Gestor sem departamento autorizado");
            }
            departamento = atual.getId();
        }
        var departamentos = jdbc.query("""
                SELECT id, nome FROM departamentos
                WHERE empresa_id = ? AND (CAST(? AS BIGINT) IS NULL OR id = ?)
                ORDER BY nome, id
                """, (rs, i) -> new Departamento(rs.getLong("id"), rs.getString("nome")),
                empresa, departamento, departamento);
        var itens = departamentos.stream().map(d -> {
            var consulta = new ResultadoMaturidadeService.Consulta(inicio, fim,
                    ResultadoMaturidadeService.Natureza.OPERACIONAL, null, d.id(), null, null);
            var apuracao = maturidade.consultar(usuario, consulta);
            return new Linha(d.id(), d.nome(), null, apuracao, classificacao.classificar(apuracao.fatores()));
        }).toList();
        return new Resposta(empresa, inicio, fim, "OPERACIONAL", ClassificacaoRiscoDepartamental.VERSAO,
                ordenar(itens, administrador));
    }

    static List<Linha> ordenar(List<Linha> linhas, boolean permitirPosicao) {
        var ordenadas = linhas.stream().sorted(Comparator
                .comparing((Linha l) -> l.apuracao().scoreGlobal(),
                        Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(Linha::departamento)
                .thenComparing(Linha::departamentoId)).toList();
        var saida = new ArrayList<Linha>();
        BigDecimal anterior = null;
        int posicao = 0;
        for (int i = 0; i < ordenadas.size(); i++) {
            var linha = ordenadas.get(i);
            var score = linha.apuracao().scoreGlobal();
            Integer lugar = null;
            if (permitirPosicao && score != null) {
                if (anterior == null || anterior.compareTo(score) != 0) posicao = i + 1;
                lugar = posicao;
                anterior = score;
            }
            saida.add(new Linha(linha.departamentoId(), linha.departamento(), lugar, linha.apuracao(), linha.risco()));
        }
        return List.copyOf(saida);
    }

    private record Departamento(Long id, String nome) {}
    public record Linha(Long departamentoId, String departamento, Integer posicao,
                        ResultadoMaturidadeService.Resultado apuracao,
                        ClassificacaoRiscoDepartamental.Resultado risco) {}
    public record Resposta(Long empresaId, LocalDate inicio, LocalDate fim, String fonte,
                           String situacaoRisco, List<Linha> departamentos) {}
}
