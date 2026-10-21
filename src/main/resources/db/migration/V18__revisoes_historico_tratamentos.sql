ALTER TABLE tratamentos_nao_conformidade ADD COLUMN resultado_vigente VARCHAR(30);
UPDATE tratamentos_nao_conformidade
SET resultado_vigente = (SELECT a.status FROM avaliacoes_conformidade a
                        WHERE a.id = tratamentos_nao_conformidade.avaliacao_id);
ALTER TABLE tratamentos_nao_conformidade ALTER COLUMN resultado_vigente SET NOT NULL;
ALTER TABLE tratamentos_nao_conformidade ADD COLUMN revisoes_avaliacao BIGINT NOT NULL DEFAULT 0;
ALTER TABLE tratamentos_nao_conformidade ADD CONSTRAINT ck_tratamento_resultado
    CHECK (resultado_vigente IN ('CONFORME','NAO_CONFORME','REQUER_REVISAO'));
ALTER TABLE tratamentos_nao_conformidade ADD CONSTRAINT ck_tratamento_revisoes CHECK (revisoes_avaliacao >= 0);

CREATE TABLE historico_tratamentos (
    id BIGSERIAL PRIMARY KEY,
    tratamento_id BIGINT NOT NULL REFERENCES tratamentos_nao_conformidade(id),
    sequencia BIGINT NOT NULL,
    tipo VARCHAR(30) NOT NULL,
    autor_id BIGINT REFERENCES usuarios(id),
    registrado_em TIMESTAMP NOT NULL,
    status_tratamento VARCHAR(30) NOT NULL,
    responsavel_id BIGINT,
    acao_corretiva VARCHAR(1000),
    resultado_anterior VARCHAR(30) NOT NULL,
    resultado_vigente VARCHAR(30) NOT NULL,
    justificativa VARCHAR(1000),
    CONSTRAINT uk_historico_tratamento_sequencia UNIQUE (tratamento_id, sequencia),
    CONSTRAINT ck_historico_sequencia CHECK (sequencia >= 0),
    CONSTRAINT ck_historico_tipo CHECK (tipo IN ('ABERTURA','ATUALIZACAO','CONFIRMACAO','REVISAO','ESTADO_MIGRADO')),
    CONSTRAINT ck_historico_status CHECK (status_tratamento IN ('ABERTA','EM_TRATAMENTO','RESOLVIDA')),
    CONSTRAINT ck_historico_anterior CHECK (resultado_anterior IN ('CONFORME','NAO_CONFORME','REQUER_REVISAO')),
    CONSTRAINT ck_historico_resultado CHECK (resultado_vigente IN ('CONFORME','NAO_CONFORME','REQUER_REVISAO')),
    CONSTRAINT ck_historico_autor CHECK (tipo = 'ESTADO_MIGRADO' OR autor_id IS NOT NULL),
    CONSTRAINT ck_historico_justificativa CHECK (
        tipo NOT IN ('CONFIRMACAO','REVISAO') OR
        (justificativa IS NOT NULL AND LENGTH(TRIM(justificativa)) > 0))
);

-- A V17 não registrava os eventos detalhados. Não inventa autor, data ou estados anteriores.
INSERT INTO historico_tratamentos
    (tratamento_id,sequencia,tipo,autor_id,registrado_em,status_tratamento,responsavel_id,
     acao_corretiva,resultado_anterior,resultado_vigente,justificativa)
SELECT id,versao_registro,'ESTADO_MIGRADO',NULL,CURRENT_TIMESTAMP,status,responsavel_id,
       acao_corretiva,resultado_vigente,resultado_vigente,
       'Estado disponível na migração. Alterações anteriores não possuem histórico detalhado.'
FROM tratamentos_nao_conformidade;
