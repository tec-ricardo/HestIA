CREATE TABLE tratamentos_nao_conformidade (
    id BIGSERIAL PRIMARY KEY,
    avaliacao_id BIGINT NOT NULL UNIQUE REFERENCES avaliacoes_conformidade(id),
    empresa_id BIGINT NOT NULL REFERENCES empresas(id),
    departamento_id BIGINT NOT NULL REFERENCES departamentos(id),
    criado_por_id BIGINT NOT NULL REFERENCES usuarios(id),
    criado_em TIMESTAMP NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'ABERTA',
    responsavel_id BIGINT REFERENCES usuarios(id),
    acao_corretiva VARCHAR(1000),
    versao_registro BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_tratamento_status CHECK (status IN ('ABERTA', 'EM_TRATAMENTO', 'RESOLVIDA'))
);
CREATE INDEX idx_tratamento_empresa_status_data
    ON tratamentos_nao_conformidade(empresa_id, status, criado_em);
CREATE INDEX idx_tratamento_empresa_departamento_data
    ON tratamentos_nao_conformidade(empresa_id, departamento_id, criado_em);
