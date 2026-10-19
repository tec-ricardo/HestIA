
package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Statement;

public class V11__adicionar_pgvector_respostas
        extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {

        String banco = context.getConnection()
                .getMetaData()
                .getDatabaseProductName();

        if (!banco.equalsIgnoreCase("PostgreSQL")) {
            return;
        }

        try (Statement statement =
                     context.getConnection().createStatement()) {

            statement.execute(
                    "CREATE EXTENSION IF NOT EXISTS vector"
            );

            statement.execute(
                    """
                    ALTER TABLE respostas_reutilizaveis
                    ADD COLUMN IF NOT EXISTS
                    embedding_vector vector(1536)
                    """
            );

            statement.execute(
                    """
                    CREATE INDEX IF NOT EXISTS
                    idx_respostas_embedding_vector
                    ON respostas_reutilizaveis
                    USING hnsw (embedding_vector vector_cosine_ops)
                    WHERE reutilizavel = true
                      AND possui_dados_sensiveis = false
                    """
            );
        }
    }
}
