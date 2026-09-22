CREATE TABLE respostas_reutilizaveis (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT,
    empresa_id BIGINT NOT NULL,
    prompt_original TEXT NOT NULL,
    resposta_gerada TEXT NOT NULL,
    embedding TEXT,
    categoria VARCHAR(255),
    modelo_ia VARCHAR(255),
    tokens_entrada INTEGER,
    tokens_saida INTEGER,
    quantidade_reutilizacoes INTEGER NOT NULL DEFAULT 0,
    tokens_economizados BIGINT NOT NULL DEFAULT 0,
    possui_dados_sensiveis BOOLEAN NOT NULL DEFAULT FALSE,
    reutilizavel BOOLEAN NOT NULL DEFAULT TRUE,
    data_criacao TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_resposta_reutilizacoes CHECK (quantidade_reutilizacoes >= 0),
    CONSTRAINT ck_resposta_tokens_economizados CHECK (tokens_economizados >= 0)
);

CREATE INDEX idx_resposta_empresa ON respostas_reutilizaveis(empresa_id);
CREATE INDEX idx_resposta_reutilizavel ON respostas_reutilizaveis(empresa_id, reutilizavel);
