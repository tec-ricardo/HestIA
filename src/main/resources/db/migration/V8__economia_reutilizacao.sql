ALTER TABLE respostas_reutilizaveis
    ADD COLUMN tokens_economizados BIGINT NOT NULL DEFAULT 0;

ALTER TABLE respostas_reutilizaveis
    ADD CONSTRAINT ck_resposta_tokens_economizados CHECK (tokens_economizados >= 0);
