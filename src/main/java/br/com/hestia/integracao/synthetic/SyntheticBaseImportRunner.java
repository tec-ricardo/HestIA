package br.com.hestia.integracao.synthetic;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

@Component
@ConditionalOnProperty(name = "hestia.synthetic-import.source")
public class SyntheticBaseImportRunner implements ApplicationRunner {

    private final DataSource dataSource;
    private final ObjectMapper objectMapper;
    private final String sourcePath;

    public SyntheticBaseImportRunner(
            DataSource dataSource,
            Environment environment
    ) {
        this.dataSource = dataSource;
        this.objectMapper = new ObjectMapper();
        this.sourcePath = environment.getRequiredProperty(
                "hestia.synthetic-import.source"
        );
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        Path sqlitePath = Path.of(sourcePath).toAbsolutePath();

        if (!Files.isRegularFile(sqlitePath)) {
            throw new IllegalArgumentException(
                    "Banco SQLite não encontrado: " + sqlitePath
            );
        }

        String origemSha256 = sha256(sqlitePath);

        try (
                Connection origem = DriverManager.getConnection(
                        "jdbc:sqlite:" + sqlitePath
                );
                Connection destino = dataSource.getConnection()
        ) {
            destino.setAutoCommit(false);

            long total = 0;

            for (String tabela : listarTabelas(origem)) {
                total += importarTabela(
                        origem,
                        destino,
                        tabela,
                        origemSha256
                );
            }

            destino.commit();

            System.out.println(
                    "Importação concluída. Linhas processadas: " + total
            );
        }
    }

    private long importarTabela(
            Connection origem,
            Connection destino,
            String tabela,
            String origemSha256
    ) throws Exception {
        String sql = """
            INSERT INTO base_sintetica.import_linhas
                (origem_sha256, tabela, chave_origem, dados)
            VALUES (?, ?, ?, CAST(? AS jsonb))
            ON CONFLICT (origem_sha256, tabela, chave_origem)
            DO UPDATE SET
                dados = EXCLUDED.dados,
                importado_em = CURRENT_TIMESTAMP
            """;

        long total = 0;
        String consulta = "SELECT rowid, * FROM " + identificador(tabela);

        try (
                Statement statement = origem.createStatement();
                ResultSet resultado = statement.executeQuery(consulta);
                PreparedStatement insert = destino.prepareStatement(sql)
        ) {
            ResultSetMetaData metadata = resultado.getMetaData();

            while (resultado.next()) {
                long rowid = resultado.getLong(1);
                ObjectNode dados = objectMapper.createObjectNode();

                for (int coluna = 2;
                     coluna <= metadata.getColumnCount();
                     coluna++) {

                    String nome = metadata.getColumnLabel(coluna);
                    Object valor = resultado.getObject(coluna);

                    JsonNode json = valor == null
                            ? NullNode.getInstance()
                            : objectMapper.valueToTree(valor);

                    dados.set(nome, json);
                }

                insert.setString(1, origemSha256);
                insert.setString(2, tabela);
                insert.setString(3, Long.toString(rowid));
                insert.setString(
                        4,
                        objectMapper.writeValueAsString(dados)
                );
                insert.addBatch();

                total++;

                if (total % 500 == 0) {
                    insert.executeBatch();
                }
            }

            insert.executeBatch();
        }

        return total;
    }

    private List<String> listarTabelas(Connection origem)
            throws SQLException {
        List<String> tabelas = new ArrayList<>();

        String sql = """
            SELECT name
            FROM sqlite_master
            WHERE type = 'table'
              AND name NOT LIKE 'sqlite_%'
            ORDER BY name
            """;

        try (
                PreparedStatement statement =
                        origem.prepareStatement(sql);
                ResultSet resultado = statement.executeQuery()
        ) {
            while (resultado.next()) {
                tabelas.add(resultado.getString("name"));
            }
        }

        return tabelas;
    }

    private String identificador(String valor) {
        return "\"" + valor.replace("\"", "\"\"") + "\"";
    }

    private String sha256(Path arquivo) throws Exception {
        byte[] conteudo = Files.readAllBytes(arquivo);
        byte[] hash = MessageDigest
                .getInstance("SHA-256")
                .digest(conteudo);

        StringBuilder resultado = new StringBuilder();

        for (byte parte : hash) {
            resultado.append(String.format("%02x", parte));
        }

        return resultado.toString();
    }
}