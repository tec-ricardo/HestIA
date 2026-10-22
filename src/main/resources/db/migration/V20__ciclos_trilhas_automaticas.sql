ALTER TABLE trilhas ADD COLUMN exige_aprovacao BOOLEAN NOT NULL DEFAULT FALSE;
CREATE TABLE trilha_ciclos_automaticos (
 id BIGSERIAL PRIMARY KEY, empresa_id BIGINT NOT NULL REFERENCES empresas(id), usuario_id BIGINT NOT NULL REFERENCES usuarios(id),
 participacao_id BIGINT NOT NULL UNIQUE REFERENCES participacoes_trilha(id), tema VARCHAR(80) NOT NULL, tipo VARCHAR(30) NOT NULL,
 regra_versao VARCHAR(80) NOT NULL, anterior_id BIGINT REFERENCES trilha_ciclos_automaticos(id),
 corte_aprendizagem TIMESTAMP, motivo VARCHAR(1000) NOT NULL, criado_em TIMESTAMP NOT NULL
);
CREATE TABLE trilha_evidencias (
 id BIGSERIAL PRIMARY KEY, ciclo_id BIGINT NOT NULL REFERENCES trilha_ciclos_automaticos(id),
 empresa_id BIGINT NOT NULL REFERENCES empresas(id), usuario_id BIGINT NOT NULL REFERENCES usuarios(id),
 chave_origem VARCHAR(160) NOT NULL, tipo_origem VARCHAR(50) NOT NULL, ocorrido_em TIMESTAMP NOT NULL, processado_em TIMESTAMP NOT NULL,
 UNIQUE(empresa_id,usuario_id,chave_origem)
);
CREATE TABLE trilha_aprendizagem_requisitos (
 trilha_id BIGINT NOT NULL REFERENCES trilhas(id), treinamento_id BIGINT NOT NULL,
 catalogo_id BIGINT NOT NULL REFERENCES aprendizagem_catalogo(id), conteudo_sha256 VARCHAR(64) NOT NULL,
 PRIMARY KEY(trilha_id,treinamento_id)
);
CREATE TABLE trilha_ocorrencias_confirmadas (
 id BIGSERIAL PRIMARY KEY, empresa_id BIGINT NOT NULL REFERENCES empresas(id), usuario_id BIGINT NOT NULL REFERENCES usuarios(id),
 avaliacao_id BIGINT NOT NULL REFERENCES avaliacoes_conformidade(id), tipo_erro VARCHAR(80) NOT NULL,
 ocorrido_em TIMESTAMP NOT NULL, confirmado_por BIGINT NOT NULL REFERENCES usuarios(id), confirmado_em TIMESTAMP NOT NULL,
 justificativa VARCHAR(1000) NOT NULL, UNIQUE(empresa_id,avaliacao_id,tipo_erro)
);
CREATE TABLE trilha_pendencias_catalogo (
 empresa_id BIGINT NOT NULL REFERENCES empresas(id), usuario_id BIGINT NOT NULL REFERENCES usuarios(id),
 tema VARCHAR(80) NOT NULL, motivo VARCHAR(1000) NOT NULL, identificada_em TIMESTAMP NOT NULL,
 PRIMARY KEY(empresa_id,usuario_id,tema)
);
CREATE INDEX idx_trilha_ciclo_tema ON trilha_ciclos_automaticos(empresa_id,usuario_id,tema);
