CREATE TABLE historicos_microtreinamento (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT
);

CREATE TABLE conclusoes_microtreinamento (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT,
    microtreinamento_id BIGINT,
    data_conclusao TIMESTAMP,
    concluido BOOLEAN NOT NULL DEFAULT FALSE,
    historico_id BIGINT,
    CONSTRAINT fk_conclusao_historico
        FOREIGN KEY (historico_id) REFERENCES historicos_microtreinamento(id)
);

CREATE TABLE necessidades_microtreinamento (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT,
    tipo_erro VARCHAR(255),
    tema_treinamento VARCHAR(255),
    data_identificacao TIMESTAMP,
    necessario BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_conclusao_microtreinamento_usuario
    ON conclusoes_microtreinamento(usuario_id);
CREATE INDEX idx_conclusao_microtreinamento_historico
    ON conclusoes_microtreinamento(historico_id);
CREATE INDEX idx_historico_microtreinamento_usuario
    ON historicos_microtreinamento(usuario_id);
CREATE INDEX idx_necessidade_microtreinamento_usuario
    ON necessidades_microtreinamento(usuario_id);
