package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Statement;

public class V13__criar_staging_base_sintetica
        extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        String banco = context.getConnection()
                .getMetaData()
                .getDatabaseProductName();

        if (!"PostgreSQL".equalsIgnoreCase(banco)) {
            return;
        }

        try (Statement statement = context.getConnection().createStatement()) {
            statement.execute("""
                CREATE SCHEMA IF NOT EXISTS base_sintetica
                """);

            statement.execute("""
                CREATE TABLE IF NOT EXISTS base_sintetica.import_linhas (
                    id BIGSERIAL PRIMARY KEY,
                    origem_sha256 CHAR(64) NOT NULL,
                    tabela VARCHAR(100) NOT NULL,
                    chave_origem VARCHAR(255) NOT NULL,
                    dados JSONB NOT NULL,
                    importado_em TIMESTAMPTZ NOT NULL
                        DEFAULT CURRENT_TIMESTAMP,
                    CONSTRAINT uk_base_sintetica_linha
                        UNIQUE (origem_sha256, tabela, chave_origem)
                )
                """);

            statement.execute("""
                CREATE INDEX IF NOT EXISTS idx_base_sintetica_tabela
                ON base_sintetica.import_linhas (tabela)
                """);
        }
    }
}