CREATE TABLE resposta_versoes (
                                  id BIGSERIAL PRIMARY KEY,
                                  resposta_id BIGINT NOT NULL,
                                  numero_versao INTEGER NOT NULL,
                                  origem VARCHAR(500) NOT NULL,
                                  tipo_conteudo VARCHAR(50) NOT NULL,
                                  conteudo_sha256 VARCHAR(64) NOT NULL,
                                  prompt_original TEXT NOT NULL,
                                  resposta_gerada TEXT NOT NULL,
                                  criada_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                  CONSTRAINT fk_resposta_versao
                                      FOREIGN KEY (resposta_id)
                                          REFERENCES respostas_reutilizaveis(id),

                                  CONSTRAINT uk_resposta_numero_versao
                                      UNIQUE (resposta_id, numero_versao),

                                  CONSTRAINT ck_resposta_numero_versao
                                      CHECK (numero_versao > 0)
);