CREATE TABLE aprendizagem_catalogo (
 id BIGSERIAL PRIMARY KEY, empresa_id BIGINT NOT NULL REFERENCES empresas(id),
 treinamento_id BIGINT NOT NULL, versao INTEGER NOT NULL CHECK(versao>0),
 conteudo_sha256 VARCHAR(64) NOT NULL, tema VARCHAR(80) NOT NULL,
 questionario TEXT NOT NULL, publicado_por BIGINT NOT NULL REFERENCES usuarios(id), publicado_em TIMESTAMP NOT NULL,
 UNIQUE(empresa_id,treinamento_id,versao)
);
CREATE TABLE aprendizagem_leituras (
 id BIGSERIAL PRIMARY KEY, empresa_id BIGINT NOT NULL REFERENCES empresas(id), usuario_id BIGINT NOT NULL REFERENCES usuarios(id),
 treinamento_id BIGINT NOT NULL, conteudo_sha256 VARCHAR(64) NOT NULL,
 conclusao_id BIGINT NOT NULL REFERENCES conclusoes_microtreinamento(id), concluida_em TIMESTAMP NOT NULL,
 chave_evento VARCHAR(100) NOT NULL, UNIQUE(empresa_id,usuario_id,chave_evento)
);
CREATE TABLE aprendizagem_ciclos (
 id BIGSERIAL PRIMARY KEY, empresa_id BIGINT NOT NULL REFERENCES empresas(id), usuario_id BIGINT NOT NULL REFERENCES usuarios(id),
 catalogo_id BIGINT NOT NULL REFERENCES aprendizagem_catalogo(id), numero INTEGER NOT NULL,
 status VARCHAR(40) NOT NULL, anterior_id BIGINT REFERENCES aprendizagem_ciclos(id),
 motivo VARCHAR(1000) NOT NULL, autorizado_por BIGINT REFERENCES usuarios(id), aberto_em TIMESTAMP NOT NULL,
 chave_liberacao VARCHAR(100), evidencias_reforco TEXT,
 UNIQUE(empresa_id,usuario_id,catalogo_id,numero), UNIQUE(empresa_id,chave_liberacao),
 CHECK(status IN ('EM_ANDAMENTO','APROVADA','AGUARDANDO_REFORCO','ENCERRADO'))
);
CREATE TABLE aprendizagem_tentativas (
 id BIGSERIAL PRIMARY KEY, ciclo_id BIGINT NOT NULL REFERENCES aprendizagem_ciclos(id), numero INTEGER NOT NULL CHECK(numero BETWEEN 1 AND 3),
 aberta_em TIMESTAMP NOT NULL, finalizada_em TIMESTAMP, questionario_preservado TEXT NOT NULL,
 respostas TEXT, acertos INTEGER, nota NUMERIC(5,2), status VARCHAR(20) NOT NULL,
 UNIQUE(ciclo_id,numero), CHECK(status IN ('EM_ANDAMENTO','APROVADA','REPROVADA')),
 CHECK(nota IS NULL OR nota BETWEEN 0 AND 100)
);
CREATE TABLE permissoes_capacitacao (
 empresa_id BIGINT NOT NULL REFERENCES empresas(id), usuario_id BIGINT NOT NULL REFERENCES usuarios(id),
 concedida_por BIGINT NOT NULL REFERENCES usuarios(id), concedida_em TIMESTAMP NOT NULL,
 PRIMARY KEY(empresa_id,usuario_id)
);
CREATE TABLE certificacao_definicoes (
 id BIGSERIAL PRIMARY KEY, empresa_id BIGINT NOT NULL REFERENCES empresas(id), codigo VARCHAR(100) NOT NULL,
 nome VARCHAR(255) NOT NULL, nivel VARCHAR(100) NOT NULL, versao INTEGER NOT NULL CHECK(versao>0), requisitos TEXT NOT NULL,
 publicado_por BIGINT NOT NULL REFERENCES usuarios(id), publicada_em TIMESTAMP NOT NULL,
 UNIQUE(empresa_id,codigo,versao)
);
CREATE TABLE certificacao_concessoes (
 id BIGSERIAL PRIMARY KEY, empresa_id BIGINT NOT NULL REFERENCES empresas(id), usuario_id BIGINT NOT NULL REFERENCES usuarios(id),
 definicao_id BIGINT NOT NULL REFERENCES certificacao_definicoes(id), codigo VARCHAR(100) NOT NULL,
 nome VARCHAR(255) NOT NULL, nivel VARCHAR(100) NOT NULL, versao INTEGER NOT NULL,
 concedida_em TIMESTAMP NOT NULL, evidencias TEXT NOT NULL,
 UNIQUE(empresa_id,usuario_id,definicao_id)
);
CREATE INDEX idx_aprendizagem_usuario ON aprendizagem_ciclos(empresa_id,usuario_id,catalogo_id);
CREATE INDEX idx_certificacao_usuario ON certificacao_concessoes(empresa_id,usuario_id);
