CREATE TABLE trilhas (
    id BIGSERIAL PRIMARY KEY, empresa_id BIGINT NOT NULL REFERENCES empresas(id), titulo VARCHAR(255) NOT NULL
);
CREATE TABLE trilha_etapas (
    trilha_id BIGINT NOT NULL REFERENCES trilhas(id), ordem INTEGER NOT NULL,
    microtreinamento_id BIGINT NOT NULL, PRIMARY KEY(trilha_id,ordem), UNIQUE(trilha_id,microtreinamento_id)
);
CREATE TABLE participacoes_trilha (
    id BIGSERIAL PRIMARY KEY, trilha_id BIGINT NOT NULL REFERENCES trilhas(id),
    usuario_id BIGINT NOT NULL REFERENCES usuarios(id), empresa_id BIGINT NOT NULL REFERENCES empresas(id),
    iniciada_em TIMESTAMP NOT NULL, concluida_em TIMESTAMP, status VARCHAR(255) NOT NULL,
    UNIQUE(trilha_id,usuario_id), CHECK(status IN ('NAO_INICIADA','EM_ANDAMENTO','CONCLUIDA'))
);
CREATE INDEX idx_trilha_usuario_empresa ON participacoes_trilha(empresa_id,usuario_id);
CREATE TABLE trilha_conclusoes (
    participacao_id BIGINT NOT NULL REFERENCES participacoes_trilha(id),
    microtreinamento_id BIGINT NOT NULL, conclusao_id BIGINT NOT NULL REFERENCES conclusoes_microtreinamento(id),
    PRIMARY KEY(participacao_id,microtreinamento_id)
);
