package br.com.hestia.integracao.synthetic;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "hestia.synthetic-operational.cnpj")
public class SyntheticOperationalImportRunner implements ApplicationRunner {

    private final DataSource dataSource;
    private final String cnpj;
    private final boolean trataDadosPessoais;

    public SyntheticOperationalImportRunner(DataSource dataSource, Environment environment) {
        this.dataSource = dataSource;
        this.cnpj = environment.getRequiredProperty("hestia.synthetic-operational.cnpj").replaceAll("\\D", "");
        this.trataDadosPessoais = Boolean.parseBoolean(
                environment.getProperty("hestia.synthetic-operational.trata-dados-pessoais", "false")
        );
        if (this.cnpj.length() != 14) {
            throw new IllegalArgumentException("hestia.synthetic-operational.cnpj deve conter 14 dígitos");
        }
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try {
                long empresaId = importarEmpresa(connection);
                Map<String, Long> departamentos = importarDepartamentos(connection, empresaId);
                importarFerramentas(connection, empresaId);
                connection.commit();
                System.out.printf("Carga operacional concluída: empresa=%d, departamentos=%d%n",
                        empresaId, departamentos.size());
            } catch (Exception exception) {
                connection.rollback();
                throw exception;
            }
        }
    }

    private long importarEmpresa(Connection connection) throws Exception {
        String consulta = """
                SELECT id
                FROM empresas
                WHERE cnpj = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(consulta)) {
            statement.setString(1, cnpj);
            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) return result.getLong("id");
            }
        }

        String nome = valorOrigem(connection, "companies", "company_id", "C01", "company_name");
        String insert = """
                INSERT INTO empresas (nome, cnpj, configuracoes_gerais, orcamento)
                VALUES (?, ?, NULL, NULL)
                RETURNING id
                """;
        try (PreparedStatement statement = connection.prepareStatement(insert)) {
            statement.setString(1, nome);
            statement.setString(2, cnpj);
            try (ResultSet result = statement.executeQuery()) {
                result.next();
                return result.getLong(1);
            }
        }
    }

    private Map<String, Long> importarDepartamentos(Connection connection, long empresaId) throws Exception {
        Map<String, Long> resultado = new HashMap<>();
        String consulta = """
                SELECT dados->>'department_id' AS origem_id,
                       dados->>'department_name' AS nome
                FROM base_sintetica.import_linhas
                WHERE tabela = 'departments'
                  AND dados->>'company_id' = 'C01'
                ORDER BY dados->>'department_id'
                """;
        try (PreparedStatement statement = connection.prepareStatement(consulta);
             ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                String origemId = result.getString("origem_id");
                String nome = result.getString("nome");
                resultado.put(origemId, buscarOuCriarDepartamento(connection, empresaId, nome));
            }
        }
        return resultado;
    }

    private long buscarOuCriarDepartamento(Connection connection, long empresaId, String nome) throws Exception {
        String consulta = """
                SELECT id
                FROM departamentos
                WHERE empresa_id = ? AND LOWER(nome) = LOWER(?)
                """;
        try (PreparedStatement statement = connection.prepareStatement(consulta)) {
            statement.setLong(1, empresaId);
            statement.setString(2, nome);
            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) return result.getLong("id");
            }
        }

        String insert = """
                INSERT INTO departamentos (nome, responsavel, estrutura_hierarquica, empresa_id)
                VALUES (?, NULL, NULL, ?)
                RETURNING id
                """;
        try (PreparedStatement statement = connection.prepareStatement(insert)) {
            statement.setString(1, nome);
            statement.setLong(2, empresaId);
            try (ResultSet result = statement.executeQuery()) {
                result.next();
                return result.getLong(1);
            }
        }
    }

    private void importarFerramentas(Connection connection, long empresaId) throws Exception {
        String consulta = """
                SELECT DISTINCT tool.dados->>'tool_name' AS nome,
                       tool.dados->>'provider' AS fornecedor,
                       tool.dados->>'tool_category' AS categoria
                FROM base_sintetica.import_linhas tool
                JOIN base_sintetica.import_linhas uso
                  ON uso.tabela = 'ai_usage_logs'
                 AND uso.dados->>'company_id' = 'C01'
                 AND uso.dados->>'ai_tool_id' = tool.dados->>'ai_tool_id'
                WHERE tool.tabela = 'ai_tools'
                ORDER BY nome
                """;
        try (PreparedStatement statement = connection.prepareStatement(consulta);
             ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                inserirFerramenta(connection, empresaId, result.getString("nome"),
                        result.getString("fornecedor"), result.getString("categoria"));
            }
        }
    }

    private void inserirFerramenta(Connection connection, long empresaId, String nome,
                                   String fornecedor, String categoria) throws Exception {
        String sql = """
                INSERT INTO ferramentas_ia (
                    nome, fornecedor, descricao, tipo, finalidade_uso, url_acesso,
                    status, nivel_risco, trata_dados_pessoais, data_cadastro,
                    data_atualizacao, empresa_id
                )
                VALUES (?, ?, ?, ?, ?, NULL, 'EM_ANALISE', 'NAO_AVALIADO', ?, CURRENT_TIMESTAMP,
                        CURRENT_TIMESTAMP, ?)
                ON CONFLICT (empresa_id, nome, fornecedor) DO UPDATE SET
                    descricao = EXCLUDED.descricao,
                    tipo = EXCLUDED.tipo,
                    finalidade_uso = EXCLUDED.finalidade_uso,
                    trata_dados_pessoais = EXCLUDED.trata_dados_pessoais,
                    data_atualizacao = CURRENT_TIMESTAMP
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, nome);
            statement.setString(2, fornecedor);
            statement.setString(3, categoria);
            statement.setString(4, mapearTipo(categoria));
            statement.setString(5, categoria);
            statement.setBoolean(6, trataDadosPessoais);
            statement.setLong(7, empresaId);
            statement.executeUpdate();
        }
    }

    private String valorOrigem(Connection connection, String tabela, String campo, String valor, String retorno)
            throws Exception {
        String sql = "SELECT dados->>? FROM base_sintetica.import_linhas WHERE tabela = ? AND dados->>? = ? LIMIT 1";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, retorno);
            statement.setString(2, tabela);
            statement.setString(3, campo);
            statement.setString(4, valor);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) throw new IllegalStateException("Registro C01 não encontrado no staging");
                return result.getString(1);
            }
        }
    }

    private String mapearTipo(String categoria) {
        return switch (categoria) {
            case "Copiloto de Desenvolvimento" -> "ASSISTENTE_CODIGO";
            case "Assistente Generativo" -> "CHATBOT";
            case "IA Integrada a Produtividade" -> "AUTOMACAO";
            case "Plataforma Corporativa de IA" -> "ANALISE_DADOS";
            case "API de Modelo" -> "OUTROS";
            default -> "OUTROS";
        };
    }
}
