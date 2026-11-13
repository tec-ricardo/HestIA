CREATE TABLE autorizacoes_ferramentas_ia (
                                             id BIGSERIAL PRIMARY KEY,
                                             empresa_id BIGINT NOT NULL REFERENCES empresas(id),
                                             ferramenta_id BIGINT NOT NULL REFERENCES ferramentas_ia(id),
                                             status VARCHAR(30) NOT NULL,
                                             inicio_vigencia TIMESTAMP NOT NULL,
                                             fim_vigencia TIMESTAMP,
                                             responsavel_id BIGINT NOT NULL REFERENCES usuarios(id),
                                             motivo VARCHAR(1000) NOT NULL,
                                             CONSTRAINT ck_autorizacao_ferramenta_status
                                                 CHECK (status IN ('APROVADA', 'DESCONTINUADA')),
                                             CONSTRAINT ck_autorizacao_ferramenta_vigencia
                                                 CHECK (fim_vigencia IS NULL OR fim_vigencia > inicio_vigencia),
                                             CONSTRAINT uk_autorizacao_ferramenta_inicio
                                                 UNIQUE (empresa_id, ferramenta_id, inicio_vigencia)
);
