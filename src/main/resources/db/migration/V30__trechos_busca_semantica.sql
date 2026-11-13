CREATE TABLE resposta_trechos_vetoriais (
 resposta_id BIGINT NOT NULL REFERENCES respostas_reutilizaveis(id) ON DELETE CASCADE,
 ordem INTEGER NOT NULL,
 texto_fonte TEXT NOT NULL,
 trecho TEXT NOT NULL,
 embedding_vector TEXT NOT NULL,
 PRIMARY KEY (resposta_id, ordem)
);
