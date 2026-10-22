ALTER TABLE registros_uso_ia ADD COLUMN origem VARCHAR(30) NOT NULL DEFAULT 'DESCONHECIDA';
ALTER TABLE registros_uso_ia ADD COLUMN evento_origem VARCHAR(160);
ALTER TABLE registros_uso_ia ADD COLUMN capturado_em TIMESTAMP;
CREATE UNIQUE INDEX uk_registro_origem_evento ON registros_uso_ia(usuario_id,origem,evento_origem);
CREATE TABLE aprendizagem_progresso (
 empresa_id BIGINT NOT NULL REFERENCES empresas(id), usuario_id BIGINT NOT NULL REFERENCES usuarios(id),
 treinamento_id BIGINT NOT NULL, conteudo_sha256 VARCHAR(64) NOT NULL, etapa INTEGER NOT NULL,
 atualizado_em TIMESTAMP NOT NULL, PRIMARY KEY(empresa_id,usuario_id,treinamento_id,conteudo_sha256)
);
