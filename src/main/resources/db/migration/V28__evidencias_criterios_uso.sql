CREATE TABLE evidencias_criterios_uso (
 registro_id BIGINT NOT NULL REFERENCES registros_uso_ia(id),
 criterio VARCHAR(40) NOT NULL,
 estado VARCHAR(30) NOT NULL,
 referencia VARCHAR(2000) NOT NULL,
 conferido_por BIGINT NOT NULL REFERENCES usuarios(id),
 conferido_em TIMESTAMP NOT NULL,
 PRIMARY KEY(registro_id,criterio),
 CHECK(estado IN ('ATENDIDO','NAO_ATENDIDO','NAO_VERIFICADO'))
);
CREATE TABLE fundamentos_criterios_avaliacao (
 registro_id BIGINT NOT NULL REFERENCES processamento_avaliacao(registro_id),
 criterio VARCHAR(40) NOT NULL,
 estado VARCHAR(30) NOT NULL,
 fundamento VARCHAR(3000) NOT NULL,
 PRIMARY KEY(registro_id,criterio)
);
