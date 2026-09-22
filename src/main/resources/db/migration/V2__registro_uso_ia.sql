CREATE TABLE registros_uso_ia (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT NOT NULL,
    ferramenta_id BIGINT NOT NULL,
    modelo_ia VARCHAR(100) NOT NULL,
    data_hora TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    finalidade VARCHAR(40) NOT NULL,
    tokens_entrada INTEGER NOT NULL DEFAULT 0,
    tokens_saida INTEGER NOT NULL DEFAULT 0,
    tokens_total INTEGER NOT NULL DEFAULT 0,
    custo_estimado NUMERIC(15, 6),
    status_execucao VARCHAR(30) NOT NULL,
    tempo_resposta_ms BIGINT,
    CONSTRAINT fk_registro_uso_usuario
        FOREIGN KEY (usuario_id) REFERENCES usuarios(id),
    CONSTRAINT fk_registro_uso_ferramenta
        FOREIGN KEY (ferramenta_id) REFERENCES ferramentas_ia(id),
    CONSTRAINT ck_registro_tokens_entrada CHECK (tokens_entrada >= 0),
    CONSTRAINT ck_registro_tokens_saida CHECK (tokens_saida >= 0),
    CONSTRAINT ck_registro_tokens_total CHECK (tokens_total >= 0),
    CONSTRAINT ck_registro_custo CHECK (custo_estimado IS NULL OR custo_estimado >= 0),
    CONSTRAINT ck_registro_tempo_resposta CHECK (tempo_resposta_ms IS NULL OR tempo_resposta_ms >= 0)
);

CREATE INDEX idx_registro_uso_usuario ON registros_uso_ia(usuario_id);
CREATE INDEX idx_registro_uso_ferramenta ON registros_uso_ia(ferramenta_id);
CREATE INDEX idx_registro_uso_data_hora ON registros_uso_ia(data_hora);
