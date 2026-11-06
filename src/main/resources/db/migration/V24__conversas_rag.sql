CREATE TABLE conversas_rag (
 id BIGSERIAL PRIMARY KEY,
 empresa_id BIGINT NOT NULL REFERENCES empresas(id),
 usuario_id BIGINT NOT NULL REFERENCES usuarios(id),
 titulo VARCHAR(120) NOT NULL,
 criada_em TIMESTAMP NOT NULL,
 UNIQUE(id, empresa_id, usuario_id)
);
ALTER TABLE interacoes_rag ADD COLUMN conversa_id BIGINT;
-- Preserve cada interação antiga sem inferir vínculos entre assuntos independentes.
INSERT INTO conversas_rag(id,empresa_id,usuario_id,titulo,criada_em)
 SELECT id,empresa_id,usuario_id,substring(pergunta,1,120),criada_em FROM interacoes_rag;
UPDATE interacoes_rag SET conversa_id=id;
SELECT setval(pg_get_serial_sequence('conversas_rag','id'),
 COALESCE((SELECT MAX(id) FROM conversas_rag),1),
 EXISTS(SELECT 1 FROM conversas_rag));
ALTER TABLE interacoes_rag ALTER COLUMN conversa_id SET NOT NULL;
ALTER TABLE interacoes_rag ADD CONSTRAINT fk_interacao_conversa_autor
 FOREIGN KEY(conversa_id,empresa_id,usuario_id) REFERENCES conversas_rag(id,empresa_id,usuario_id);
CREATE INDEX idx_conversas_autor ON conversas_rag(empresa_id,usuario_id,id);
CREATE INDEX idx_interacoes_conversa ON interacoes_rag(conversa_id,id);
