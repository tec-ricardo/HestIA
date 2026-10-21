ALTER TABLE politicas_uso ADD COLUMN revisao INTEGER NOT NULL DEFAULT 1;
ALTER TABLE politicas_uso ADD COLUMN excluida BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE politicas_uso ADD COLUMN versao_registro BIGINT NOT NULL DEFAULT 0;

CREATE TABLE politica_uso_versoes (
    id BIGSERIAL PRIMARY KEY,
    politica_uso_id BIGINT NOT NULL REFERENCES politicas_uso(id),
    empresa_id BIGINT NOT NULL REFERENCES empresas(id),
    revisao INTEGER NOT NULL,
    titulo VARCHAR(150) NOT NULL,
    descricao VARCHAR(500) NOT NULL,
    conteudo TEXT NOT NULL,
    versao VARCHAR(20) NOT NULL,
    ativa BOOLEAN NOT NULL,
    excluida BOOLEAN NOT NULL,
    origem VARCHAR(30) NOT NULL,
    registrada_em TIMESTAMP NOT NULL,
    autor_id BIGINT,
    CONSTRAINT uk_politica_revisao UNIQUE (politica_uso_id, revisao),
    CONSTRAINT ck_politica_revisao CHECK (revisao > 0)
);

-- Preserva somente o estado conhecido no momento da migração.
-- Não atribui esse estado como histórico de avaliações preexistentes.
INSERT INTO politica_uso_versoes
    (politica_uso_id, empresa_id, revisao, titulo, descricao, conteudo,
     versao, ativa, excluida, origem, registrada_em)
SELECT id, empresa_id, revisao, titulo, descricao, conteudo,
       versao, ativa, excluida, 'MIGRACAO', CURRENT_TIMESTAMP
FROM politicas_uso;

ALTER TABLE avaliacoes_conformidade ADD COLUMN politica_versao_id BIGINT REFERENCES politica_uso_versoes(id);
ALTER TABLE avaliacoes_conformidade ADD COLUMN regra_codigo VARCHAR(60);
ALTER TABLE avaliacoes_conformidade ADD COLUMN regra_versao VARCHAR(20);
ALTER TABLE avaliacoes_conformidade ADD COLUMN motivo_preservado TEXT;
ALTER TABLE avaliacoes_conformidade ADD COLUMN regra_preservada TEXT;
ALTER TABLE avaliacoes_conformidade ADD COLUMN limitacoes_preservadas TEXT;
CREATE INDEX idx_avaliacao_politica_versao ON avaliacoes_conformidade(politica_versao_id);
