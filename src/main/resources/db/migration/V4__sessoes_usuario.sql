CREATE TABLE sessoes_usuario (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT NOT NULL,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    criada_em TIMESTAMP NOT NULL,
    expira_em TIMESTAMP NOT NULL,
    revogada_em TIMESTAMP,
    CONSTRAINT fk_sessao_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id)
);

CREATE UNIQUE INDEX idx_sessao_token ON sessoes_usuario(token_hash);
CREATE INDEX idx_sessao_usuario ON sessoes_usuario(usuario_id);
CREATE INDEX idx_sessao_expiracao ON sessoes_usuario(expira_em);
