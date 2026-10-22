CREATE TABLE interacoes_rag (
 id BIGSERIAL PRIMARY KEY, empresa_id BIGINT NOT NULL REFERENCES empresas(id), usuario_id BIGINT NOT NULL REFERENCES usuarios(id),
 chave VARCHAR(100) NOT NULL, pergunta TEXT NOT NULL, modo VARCHAR(30) NOT NULL, estado VARCHAR(60) NOT NULL,
 resposta TEXT, fontes TEXT NOT NULL, modelo VARCHAR(150), tokens_entrada BIGINT, tokens_saida BIGINT,
 registro_uso_id BIGINT REFERENCES registros_uso_ia(id), criada_em TIMESTAMP NOT NULL,
 UNIQUE(empresa_id,usuario_id,chave)
);
CREATE TABLE maturidade_apuracoes (
 id BIGSERIAL PRIMARY KEY, empresa_id BIGINT NOT NULL REFERENCES empresas(id), departamento_id BIGINT REFERENCES departamentos(id),
 autor_id BIGINT NOT NULL REFERENCES usuarios(id), inicio DATE NOT NULL, fim DATE NOT NULL,
 fonte VARCHAR(30) NOT NULL, snapshot VARCHAR(64), versao_pesos VARCHAR(50) NOT NULL,
 versao_calculo VARCHAR(80) NOT NULL, versao_parametros VARCHAR(100) NOT NULL,
 conteudo TEXT NOT NULL, conteudo_sha256 VARCHAR(64) NOT NULL, registrada_em TIMESTAMP NOT NULL
);
CREATE INDEX idx_apuracao_escopo ON maturidade_apuracoes(empresa_id,departamento_id,inicio,fim);
CREATE INDEX idx_rag_usuario ON interacoes_rag(empresa_id,usuario_id,criada_em);
