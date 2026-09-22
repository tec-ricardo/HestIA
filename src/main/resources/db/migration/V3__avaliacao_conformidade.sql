CREATE TABLE avaliacoes_conformidade (
    id BIGSERIAL PRIMARY KEY,
    registro_uso_id BIGINT NOT NULL UNIQUE,
    politica_uso_id BIGINT NOT NULL,
    status VARCHAR(30) NOT NULL,
    justificativa VARCHAR(1000),
    data_avaliacao TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_avaliacao_registro FOREIGN KEY (registro_uso_id) REFERENCES registros_uso_ia(id),
    CONSTRAINT fk_avaliacao_politica FOREIGN KEY (politica_uso_id) REFERENCES politicas_uso(id)
);

CREATE TABLE avaliacao_criterios_atendidos (
    avaliacao_id BIGINT NOT NULL,
    criterio VARCHAR(40) NOT NULL,
    PRIMARY KEY (avaliacao_id, criterio),
    CONSTRAINT fk_criterio_avaliacao
        FOREIGN KEY (avaliacao_id) REFERENCES avaliacoes_conformidade(id) ON DELETE CASCADE
);

CREATE INDEX idx_avaliacao_politica ON avaliacoes_conformidade(politica_uso_id);
