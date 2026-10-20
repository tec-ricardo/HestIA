CREATE TABLE recuperacoes_senha (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT NOT NULL REFERENCES usuarios(id),
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    criada_em TIMESTAMP NOT NULL,
    expira_em TIMESTAMP NOT NULL,
    utilizada_em TIMESTAMP NULL
);

CREATE INDEX idx_recuperacoes_senha_usuario
    ON recuperacoes_senha(usuario_id);