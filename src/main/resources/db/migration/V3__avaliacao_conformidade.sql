ALTER TABLE avaliacoes_conformidade
    ALTER COLUMN conforme SET DEFAULT FALSE;

ALTER TABLE avaliacoes_conformidade
    ADD COLUMN registro_uso_id BIGINT,
    ADD COLUMN politica_uso_id BIGINT,
    ADD COLUMN status VARCHAR(30);

UPDATE avaliacoes_conformidade
SET status = CASE WHEN conforme THEN 'CONFORME' ELSE 'NAO_CONFORME' END
WHERE status IS NULL;

ALTER TABLE avaliacoes_conformidade
    ALTER COLUMN status SET NOT NULL,
    ADD CONSTRAINT uk_avaliacao_registro UNIQUE (registro_uso_id),
    ADD CONSTRAINT fk_avaliacao_registro FOREIGN KEY (registro_uso_id) REFERENCES registros_uso_ia(id),
    ADD CONSTRAINT fk_avaliacao_politica FOREIGN KEY (politica_uso_id) REFERENCES politicas_uso(id);

CREATE TABLE avaliacao_criterios_atendidos (
    avaliacao_id BIGINT NOT NULL,
    criterio VARCHAR(40) NOT NULL,
    PRIMARY KEY (avaliacao_id, criterio),
    CONSTRAINT fk_criterio_avaliacao
        FOREIGN KEY (avaliacao_id) REFERENCES avaliacoes_conformidade(id) ON DELETE CASCADE
);

CREATE INDEX idx_avaliacao_politica ON avaliacoes_conformidade(politica_uso_id);
