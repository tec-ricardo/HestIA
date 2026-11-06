CREATE TABLE conversas_rag (
 id BIGSERIAL PRIMARY KEY,
 empresa_id BIGINT NOT NULL REFERENCES empresas(id),
 usuario_id BIGINT NOT NULL REFERENCES usuarios(id),
 titulo VARCHAR(120) NOT NULL,
 criada_em TIMESTAMP NOT NULL,
 interacao_legada_id BIGINT UNIQUE,
 UNIQUE(id, empresa_id, usuario_id)
);
ALTER TABLE interacoes_rag ADD COLUMN conversa_id BIGINT;
-- IDs gerados normalmente mantêm a sequência válida em PostgreSQL e H2.
-- O vínculo temporário preserva cada interação sem inferir assuntos compartilhados.
INSERT INTO conversas_rag(empresa_id,usuario_id,titulo,criada_em,interacao_legada_id)
 SELECT empresa_id,usuario_id,substring(pergunta,1,120),criada_em,id
 FROM interacoes_rag ORDER BY id;
UPDATE interacoes_rag SET conversa_id=(
 SELECT c.id FROM conversas_rag c WHERE c.interacao_legada_id=interacoes_rag.id
);
ALTER TABLE conversas_rag DROP COLUMN interacao_legada_id;
ALTER TABLE interacoes_rag ALTER COLUMN conversa_id SET NOT NULL;
ALTER TABLE interacoes_rag ADD CONSTRAINT fk_interacao_conversa_autor
 FOREIGN KEY(conversa_id,empresa_id,usuario_id) REFERENCES conversas_rag(id,empresa_id,usuario_id);
CREATE INDEX idx_conversas_autor ON conversas_rag(empresa_id,usuario_id,id);
CREATE INDEX idx_interacoes_conversa ON interacoes_rag(conversa_id,id);
