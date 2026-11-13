CREATE TABLE ciencias_politica (
 id BIGSERIAL PRIMARY KEY,
 empresa_id BIGINT NOT NULL REFERENCES empresas(id),
 usuario_id BIGINT NOT NULL REFERENCES usuarios(id),
 politica_versao_id BIGINT NOT NULL REFERENCES politica_uso_versoes(id),
 registrada_em TIMESTAMP NOT NULL,
 CONSTRAINT uk_ciencia_usuario_versao UNIQUE(usuario_id, politica_versao_id)
);
