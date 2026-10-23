package br.com.hestia.demo;

import br.com.hestia.microtreinamento.service.MicrotreinamentoFicticioService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;

/** Carga repetível e isolada para a apresentação do produto. */
@Component
@Profile("demo")
@ConditionalOnProperty(name = "hestia.demo.enabled", havingValue = "true")
public class DemoDataInitializer implements ApplicationRunner {
    public static final String EMPRESA_CNPJ = "99999999000191";
    public static final String SENHA = "Hestia@2026";

    private final JdbcTemplate jdbc;
    private final PasswordEncoder passwordEncoder;

    public DemoDataInitializer(JdbcTemplate jdbc, PasswordEncoder passwordEncoder) {
        this.jdbc = jdbc;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        long empresa = empresa();
        long tecnologia = departamento(empresa, "Tecnologia", "Marina Costa");
        long governanca = departamento(empresa, "Governança e Riscos", "Lucas Almeida");
        long admin = usuario(empresa, governanca, "Administrador Demo", "admin.demo@hestia.local", "ADMIN", "Administrador");
        long gestor = usuario(empresa, tecnologia, "Gestor Demo", "gestor.demo@hestia.local", "GESTOR", "Gestor de Tecnologia");
        long colaborador = usuario(empresa, tecnologia, "Colaborador Demo", "colaborador.demo@hestia.local", "FUNCIONARIO", "Analista");
        long ferramenta = ferramenta(empresa);
        long politica = politica(empresa, admin);

        registro(colaborador, ferramenta, "demo-uso-001", "SUCESSO", 420, 180, 0.0084);
        long registroNaoConforme = registro(colaborador, ferramenta, "demo-uso-002", "SUCESSO", 610, 250, 0.0127);
        registro(gestor, ferramenta, "demo-uso-003", "SUCESSO", 280, 120, 0.0056);
        avaliacaoNaoConforme(registroNaoConforme, politica, admin, tecnologia, gestor, empresa);
        respostaReutilizavel(empresa, admin);
        aprendizagem(empresa, admin, colaborador);
        gamificacao(colaborador);
        maturidade(empresa, tecnologia, admin);
    }

    private long empresa() {
        jdbc.update("""
            INSERT INTO empresas(nome,cnpj,configuracoes_gerais,orcamento)
            VALUES ('QSoft — Ambiente de Demonstração',?,'Carga demonstrativa isolada; não usar em produção',25000)
            ON CONFLICT (cnpj) DO UPDATE SET nome=EXCLUDED.nome
            """, EMPRESA_CNPJ);
        return id("SELECT id FROM empresas WHERE cnpj=?", EMPRESA_CNPJ);
    }

    private long departamento(long empresa, String nome, String responsavel) {
        Long existente = primeiro("SELECT id FROM departamentos WHERE empresa_id=? AND nome=?", empresa, nome);
        if (existente != null) return existente;
        jdbc.update("INSERT INTO departamentos(nome,responsavel,estrutura_hierarquica,empresa_id) VALUES(?,?,?,?)",
                nome, responsavel, "QSoft / " + nome, empresa);
        return id("SELECT id FROM departamentos WHERE empresa_id=? AND nome=?", empresa, nome);
    }

    private long usuario(long empresa, long departamento, String nome, String email, String perfil, String cargo) {
        Long existente = primeiro("SELECT id FROM usuarios WHERE email=?", email);
        if (existente == null) {
            jdbc.update("""
                INSERT INTO usuarios(nome,email,senha,cargo,perfil,ativo,data_cadastro,empresa_id,departamento_id)
                VALUES(?,?,?,?,?,true,?,?,?)
                """, nome, email, passwordEncoder.encode(SENHA), cargo, perfil, LocalDateTime.now(), empresa, departamento);
        } else {
            jdbc.update("UPDATE usuarios SET nome=?,cargo=?,perfil=?,ativo=true,empresa_id=?,departamento_id=? WHERE id=?",
                    nome, cargo, perfil, empresa, departamento, existente);
        }
        return id("SELECT id FROM usuarios WHERE email=?", email);
    }

    private long ferramenta(long empresa) {
        jdbc.update("""
            INSERT INTO ferramentas_ia(nome,fornecedor,descricao,tipo,finalidade_uso,url_acesso,status,nivel_risco,
              trata_dados_pessoais,data_cadastro,data_atualizacao,empresa_id)
            VALUES ('ChatGPT Enterprise','OpenAI','Assistente aprovado para produtividade','CHATBOT',
              'Apoio à redação, síntese e pesquisa interna','https://chatgpt.com','APROVADA','MEDIO',false,?,?,?)
            ON CONFLICT (empresa_id,nome,fornecedor) DO UPDATE SET
              tipo=EXCLUDED.tipo,status='APROVADA',data_atualizacao=EXCLUDED.data_atualizacao
            """, LocalDateTime.now(), LocalDateTime.now(), empresa);
        return id("SELECT id FROM ferramentas_ia WHERE empresa_id=? AND nome='ChatGPT Enterprise'", empresa);
    }

    private long politica(long empresa, long autor) {
        Long existente = primeiro("SELECT id FROM politicas_uso WHERE empresa_id=? AND titulo='Política de Uso Responsável de IA'", empresa);
        if (existente == null) {
            jdbc.update("""
                INSERT INTO politicas_uso(titulo,descricao,conteudo,versao,ativa,data_criacao,data_atualizacao,empresa_id,revisao,excluida,versao_registro)
                VALUES ('Política de Uso Responsável de IA','Diretrizes oficiais da demonstração',
                'Utilize somente ferramentas aprovadas. Não informe dados pessoais ou confidenciais. Valide respostas antes do uso e registre a finalidade.',
                '1.0',true,?,?,?,1,false,0)
                """, LocalDateTime.now(), LocalDateTime.now(), empresa);
            existente = id("SELECT id FROM politicas_uso WHERE empresa_id=? AND titulo='Política de Uso Responsável de IA'", empresa);
        }
        if (primeiro("SELECT id FROM politica_uso_versoes WHERE politica_uso_id=? AND revisao=1", existente) == null) {
            jdbc.update("""
                INSERT INTO politica_uso_versoes(politica_uso_id,empresa_id,revisao,titulo,descricao,conteudo,versao,ativa,excluida,origem,registrada_em,autor_id)
                SELECT id,empresa_id,1,titulo,descricao,conteudo,versao,ativa,excluida,'DEMO',?,? FROM politicas_uso WHERE id=?
                """, LocalDateTime.now(), autor, existente);
        }
        return existente;
    }

    private long registro(long usuario, long ferramenta, String evento, String status, int entrada, int saida, double custo) {
        Long existente = primeiro("SELECT id FROM registros_uso_ia WHERE usuario_id=? AND evento_origem=?", usuario, evento);
        if (existente == null) {
            jdbc.update("""
                INSERT INTO registros_uso_ia(usuario_id,ferramenta_id,modelo_ia,data_hora,finalidade,tokens_entrada,tokens_saida,
                tokens_total,custo_estimado,status_execucao,tempo_resposta_ms,origem,evento_origem,capturado_em)
                VALUES(?,?, 'gpt-4.1-mini',?,'PESQUISA',?,?,?,?,? ,1250,'IMPORTADA',?,?)
                """, usuario, ferramenta, LocalDateTime.now().minusDays(2), entrada, saida, entrada + saida, custo, status, evento, LocalDateTime.now());
        }
        jdbc.update("""
                UPDATE registros_uso_ia
                   SET finalidade='PESQUISA', ferramenta_id=?, modelo_ia='gpt-4.1-mini',
                       status_execucao='SUCESSO', origem='IMPORTADA'
                 WHERE usuario_id=? AND evento_origem=?
                """, ferramenta, usuario, evento);
        return id("SELECT id FROM registros_uso_ia WHERE usuario_id=? AND evento_origem=?", usuario, evento);
    }

    private void avaliacaoNaoConforme(long registro, long politica, long admin, long departamento, long gestor, long empresa) {
        Long avaliacao = primeiro("SELECT id FROM avaliacoes_conformidade WHERE registro_uso_id=?", registro);
        long versao = id("SELECT id FROM politica_uso_versoes WHERE politica_uso_id=? AND revisao=1", politica);
        if (avaliacao == null) {
            jdbc.update("""
                INSERT INTO avaliacoes_conformidade(registro_uso_id,politica_uso_id,status,justificativa,data_avaliacao,
                politica_versao_id,regra_codigo,regra_versao,motivo_preservado,regra_preservada,limitacoes_preservadas)
                VALUES(?,?,'NAO_CONFORME','Uso sem evidência de validação humana',?,?, 'CONFORMIDADE_CRITERIOS','1.0',
                'Validação humana não registrada','Exige ferramenta autorizada, finalidade e validação humana','Avaliação por checklist; não substitui análise jurídica')
                """, registro, politica, LocalDateTime.now().minusDays(1), versao);
            avaliacao = id("SELECT id FROM avaliacoes_conformidade WHERE registro_uso_id=?", registro);
        }
        if (primeiro("SELECT id FROM tratamentos_nao_conformidade WHERE avaliacao_id=?", avaliacao) == null) {
            jdbc.update("""
                INSERT INTO tratamentos_nao_conformidade(avaliacao_id,empresa_id,departamento_id,criado_por_id,criado_em,status,
                responsavel_id,acao_corretiva,versao_registro)
                VALUES(?,?,?,?,?,'EM_TRATAMENTO',?,'Concluir microtreinamento de validação humana e revisar o uso registrado',0)
                """, avaliacao, empresa, departamento, admin, LocalDateTime.now(), gestor);
        }
    }

    private void respostaReutilizavel(long empresa, long admin) {
        String prompt = "Quais cuidados devo tomar antes de usar IA com dados corporativos?";
        Long resposta = primeiro("SELECT id FROM respostas_reutilizaveis WHERE empresa_id=? AND prompt_original=?", empresa, prompt);
        String conteudo = "Use apenas ferramentas aprovadas, remova dados pessoais e confidenciais, confira a política vigente e valide a resposta antes de aplicá-la.";
        if (resposta == null) {
            jdbc.update("""
                INSERT INTO respostas_reutilizaveis(usuario_id,empresa_id,prompt_original,resposta_gerada,categoria,modelo_ia,
                tokens_entrada,tokens_saida,quantidade_reutilizacoes,tokens_economizados,possui_dados_sensiveis,reutilizavel,
                data_criacao,custo_estimado,status_qualidade,expira_em)
                VALUES(?,?,?,?, 'GOVERNANCA','conteudo-curado',80,65,4,260,false,true,?,0.0012,'APROVADA',?)
                """, admin, empresa, prompt, conteudo, LocalDateTime.now(), LocalDateTime.now().plusMonths(6));
            resposta = id("SELECT id FROM respostas_reutilizaveis WHERE empresa_id=? AND prompt_original=?", empresa, prompt);
        }
        for (String perfil : new String[]{"ADMIN", "GESTOR", "FUNCIONARIO"}) {
            if (primeiro("SELECT resposta_id FROM resposta_perfis WHERE resposta_id=? AND perfil=?", resposta, perfil) == null)
                jdbc.update("INSERT INTO resposta_perfis(resposta_id,perfil) VALUES(?,?)", resposta, perfil);
        }
        if (primeiro("SELECT id FROM resposta_versoes WHERE resposta_id=? AND numero_versao=1", resposta) == null)
            jdbc.update("""
                INSERT INTO resposta_versoes(resposta_id,numero_versao,origem,tipo_conteudo,conteudo_sha256,prompt_original,resposta_gerada,criada_em)
                VALUES(?,1,'DEMO','RESPOSTA_CURADA',?,?,?,?)
                """, resposta, sha256(prompt + "\n" + conteudo), prompt, conteudo, LocalDateTime.now());
    }

    private void aprendizagem(long empresa, long admin, long colaborador) {
        var treinamento = new MicrotreinamentoFicticioService().criarTreinamentos().get(0);
        String hash = sha256(treinamento.getConteudo());
        String questionario = """
            [{"id":"q1","enunciado":"O que não deve ser informado a uma IA?","alternativas":{"A":"Senhas e dados bancários","B":"Texto público"},"correta":"A","explicacao":"Credenciais e dados financeiros são protegidos."},
            {"id":"q2","enunciado":"Qual é a prática recomendada?","alternativas":{"A":"Copiar toda a base","B":"Anonimizar dados"},"correta":"B","explicacao":"A anonimização reduz riscos."},
            {"id":"q3","enunciado":"Quando usar um dado?","alternativas":{"A":"Quando necessário","B":"Sempre"},"correta":"A","explicacao":"Use somente o mínimo necessário."},
            {"id":"q4","enunciado":"O que fazer em caso de dúvida?","alternativas":{"A":"Ignorar","B":"Consultar a política"},"correta":"B","explicacao":"A política orienta o uso."},
            {"id":"q5","enunciado":"Dados de clientes podem ser compartilhados livremente?","alternativas":{"A":"Não","B":"Sim"},"correta":"A","explicacao":"Dados corporativos exigem autorização."}]
            """;
        Long catalogo = primeiro("SELECT id FROM aprendizagem_catalogo WHERE empresa_id=? AND treinamento_id=1 AND versao=1", empresa);
        if (catalogo == null) {
            jdbc.update("""
                INSERT INTO aprendizagem_catalogo(empresa_id,treinamento_id,versao,conteudo_sha256,tema,questionario,publicado_por,publicado_em)
                VALUES(?,1,1,?,'PROTECAO_DE_DADOS',?,?,?)
                """, empresa, hash, questionario, admin, LocalDateTime.now());
            catalogo = id("SELECT id FROM aprendizagem_catalogo WHERE empresa_id=? AND treinamento_id=1 AND versao=1", empresa);
        }
        if (primeiro("SELECT usuario_id FROM permissoes_capacitacao WHERE empresa_id=? AND usuario_id=?", empresa, colaborador) == null)
            jdbc.update("INSERT INTO permissoes_capacitacao(empresa_id,usuario_id,concedida_por,concedida_em) VALUES(?,?,?,?)",
                    empresa, colaborador, admin, LocalDateTime.now());
        if (primeiro("SELECT id FROM certificacao_definicoes WHERE empresa_id=? AND codigo='IA-RESPONSAVEL' AND versao=1", empresa) == null)
            jdbc.update("""
                INSERT INTO certificacao_definicoes(empresa_id,codigo,nome,nivel,versao,requisitos,publicado_por,publicada_em)
                VALUES(?,'IA-RESPONSAVEL','Fundamentos de IA Responsável','BÁSICO',1,?,?,?)
                """, empresa, "{\"catalogos\":[" + catalogo + "]}", admin, LocalDateTime.now());
    }

    private void gamificacao(long usuario) {
        if (primeiro("SELECT id FROM progressos_usuario WHERE usuario_id=?", usuario) == null)
            jdbc.update("INSERT INTO progressos_usuario(usuario_id,xp_total,nivel,atualizado_em) VALUES(?,120,'CONSCIENTE',?)", usuario, LocalDateTime.now());
        jdbc.update("UPDATE progressos_usuario SET nivel='CONSCIENTE' WHERE usuario_id=? AND nivel='PRATICANTE'", usuario);
        jdbc.update("""
            INSERT INTO progressos_missao(usuario_id,missao_id,progresso,recompensa_concedida)
            SELECT ?,id,CASE codigo WHEN 'PRIMEIROS_REGISTROS' THEN 2 ELSE 1 END,false FROM missoes
            ON CONFLICT (usuario_id,missao_id) DO NOTHING
            """, usuario);
    }

    private void maturidade(long empresa, long departamento, long admin) {
        if (primeiro("SELECT id FROM maturidade_apuracoes WHERE empresa_id=? AND fonte='DEMO'", empresa) == null) {
            String conteudo = "{\"pontuacaoGeral\":62,\"nivel\":\"EM_DESENVOLVIMENTO\",\"dimensoes\":{\"governanca\":70,\"capacitacao\":55,\"controle\":60}}";
            jdbc.update("""
                INSERT INTO maturidade_apuracoes(empresa_id,departamento_id,autor_id,inicio,fim,fonte,snapshot,versao_pesos,
                versao_calculo,versao_parametros,conteudo,conteudo_sha256,registrada_em)
                VALUES(?,?,?,CURRENT_DATE-30,CURRENT_DATE,'DEMO',NULL,'1.0','MATURIDADE_V1','demo-v1',?,?,?)
                """, empresa, departamento, admin, conteudo, sha256(conteudo), LocalDateTime.now());
        }
    }

    private Long primeiro(String sql, Object... args) {
        var ids = jdbc.query(sql, (rs, row) -> rs.getLong(1), args);
        return ids.isEmpty() ? null : ids.get(0);
    }

    private long id(String sql, Object... args) {
        Long valor = primeiro(sql, args);
        if (valor == null) throw new IllegalStateException("Carga demo não encontrou o registro esperado.");
        return valor;
    }

    private String sha256(String valor) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(valor.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("Não foi possível calcular o hash da carga demo.", e);
        }
    }
}
