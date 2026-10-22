package br.com.hestia.microtreinamento.service;

import br.com.hestia.aprendizagem.AcessoCapacitacao;
import br.com.hestia.microtreinamento.model.TipoErro;
import br.com.hestia.microtreinamento.repository.ConclusaoMicrotreinamentoRepository;
import br.com.hestia.usuario.model.Usuario;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

@Service
public class EvolucaoMicrotreinamentoService {

    private final ConclusaoMicrotreinamentoRepository conclusoes;
    private final AcessoCapacitacao acesso;
    private final ConsultaEfetividadeCapacitacaoService efetividade;
    private final JdbcTemplate jdbc;

    public EvolucaoMicrotreinamentoService(
            ConclusaoMicrotreinamentoRepository conclusoes,
            AcessoCapacitacao acesso,
            ConsultaEfetividadeCapacitacaoService efetividade,
            JdbcTemplate jdbc
    ) {
        this.conclusoes = conclusoes;
        this.acesso = acesso;
        this.efetividade = efetividade;
        this.jdbc = jdbc;
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public Resultado consultar(Usuario autenticado, Long conclusaoId, TipoErro tipoErro) {
        Long empresaId = acesso.empresa(autenticado);
        if (conclusaoId == null || conclusaoId <= 0 || tipoErro == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Parâmetros de evolução inválidos.");
        }
        var conclusao = conclusoes.findById(conclusaoId)
                .filter(item -> item.getUsuarioId() != null)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Conclusão indisponível."
                ));
        acesso.usuario(autenticado, conclusao.getUsuarioId());

        var comportamento = efetividade.consultar(autenticado, conclusaoId, tipoErro);
        var referencia = comportamento.indicador().acompanhamento().recorrencia();
        var limite = referencia.observadoAte().isBefore(referencia.fimDepois())
                ? referencia.observadoAte() : referencia.fimDepois();

        var linhas = jdbc.query("""
                SELECT a.id AS catalogo_id, a.versao, a.conteudo_sha256,
                       t.id AS tentativa_id, t.ciclo_id, t.nota, t.status, t.finalizada_em
                FROM aprendizagem_tentativas t
                JOIN aprendizagem_ciclos c ON c.id = t.ciclo_id
                JOIN aprendizagem_catalogo a ON a.id = c.catalogo_id
                WHERE c.empresa_id = ? AND c.usuario_id = ?
                  AND a.empresa_id = c.empresa_id AND a.treinamento_id = ?
                  AND t.status IN ('APROVADA', 'REPROVADA')
                  AND t.nota IS NOT NULL AND t.nota BETWEEN 0 AND 100
                  AND t.finalizada_em >= ? AND t.finalizada_em <= ?
                  AND t.finalizada_em < ? AND t.aberta_em <= t.finalizada_em
                  AND EXISTS (
                      SELECT 1 FROM aprendizagem_leituras l
                      WHERE l.empresa_id = c.empresa_id AND l.usuario_id = c.usuario_id
                        AND l.treinamento_id = a.treinamento_id
                        AND l.conclusao_id = ? AND l.conteudo_sha256 = a.conteudo_sha256
                        AND l.concluida_em <= t.aberta_em
                  )
                ORDER BY a.id, t.finalizada_em, t.id
                """, (rs, numero) -> new Linha(
                        rs.getLong("catalogo_id"), rs.getInt("versao"),
                        rs.getString("conteudo_sha256"),
                        new Tentativa(rs.getLong("tentativa_id"), rs.getLong("ciclo_id"),
                                rs.getBigDecimal("nota"), rs.getString("status"),
                                rs.getTimestamp("finalizada_em").toLocalDateTime())
                ), empresaId, referencia.usuarioId(),
                comportamento.indicador().acompanhamento().microtreinamentoId(),
                Timestamp.valueOf(referencia.dataConclusao()), Timestamp.valueOf(limite),
                Timestamp.valueOf(referencia.fimDepois()), conclusaoId);

        var grupos = new LinkedHashMap<Long, List<Linha>>();
        for (var linha : linhas) {
            grupos.computeIfAbsent(linha.catalogoId(), chave -> new ArrayList<>()).add(linha);
        }
        var aprendizagem = grupos.values().stream().map(this::comparar).toList();
        String situacao = aprendizagem.stream().anyMatch(item -> item.variacaoPontos() != null)
                ? "COMPARACAO_DISPONIVEL" : "DADOS_INSUFICIENTES";

        return new Resultado(
                conclusaoId, empresaId, referencia.usuarioId(),
                referencia.dataConclusao(), limite, referencia.periodoPosteriorCompleto(),
                situacao, aprendizagem, comportamento,
                List.of(
                        "Notas comparam a primeira e a última tentativa finalizada da mesma avaliação versionada após a leitura vinculada à conclusão.",
                        "A janela posterior é a mesma da recorrência; nenhuma nota anterior ao treinamento é presumida.",
                        "Variação de nota e recorrência são resultados distintos; não comprovam efeito causal do microtreinamento nem alteram aprovações ou certificações."
                )
        );
    }

    private Aprendizagem comparar(List<Linha> linhas) {
        var primeira = linhas.getFirst();
        var tentativas = linhas.stream().map(Linha::tentativa).toList();
        BigDecimal variacao = tentativas.size() < 2 ? null
                : tentativas.getLast().nota().subtract(tentativas.getFirst().nota());
        String situacao = variacao == null ? "DADOS_INSUFICIENTES"
                : variacao.signum() > 0 ? "MELHORIA"
                : variacao.signum() < 0 ? "PIORA" : "ESTABILIDADE";
        return new Aprendizagem(primeira.catalogoId(), primeira.versao(), primeira.hash(),
                situacao, variacao, tentativas);
    }

    private record Linha(Long catalogoId, int versao, String hash, Tentativa tentativa) {}

    public record Tentativa(Long id, Long cicloId, BigDecimal nota, String status,
                            LocalDateTime finalizadaEm) {}

    public record Aprendizagem(Long catalogoId, int versao, String conteudoSha256,
                               String situacao, BigDecimal variacaoPontos,
                               List<Tentativa> tentativas) {
        public Aprendizagem { tentativas = List.copyOf(tentativas); }
    }

    public record Resultado(Long conclusaoId, Long empresaId, Long usuarioId,
                            LocalDateTime inicio, LocalDateTime observadoAte,
                            boolean periodoPosteriorCompleto, String situacaoAprendizagem,
                            List<Aprendizagem> aprendizagem,
                            ConsultaEfetividadeCapacitacaoService.Resultado recorrencia,
                            List<String> limitacoes) {
        public Resultado {
            aprendizagem = List.copyOf(aprendizagem);
            limitacoes = List.copyOf(limitacoes);
        }
    }
}
