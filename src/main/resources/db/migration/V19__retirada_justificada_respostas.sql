-- V18 está reservada à revisão de tratamentos (SCRUM-372/373), que deve ser integrada antes desta entrega.
ALTER TABLE respostas_reutilizaveis ADD COLUMN retirada_por_id BIGINT;
ALTER TABLE respostas_reutilizaveis ADD COLUMN retirada_em TIMESTAMP;
ALTER TABLE respostas_reutilizaveis ADD COLUMN justificativa_retirada VARCHAR(1000);
ALTER TABLE respostas_reutilizaveis ADD CONSTRAINT ck_resposta_retirada_documentada CHECK (
    (retirada_por_id IS NULL AND retirada_em IS NULL AND justificativa_retirada IS NULL)
    OR (retirada_por_id IS NOT NULL AND retirada_em IS NOT NULL
        AND justificativa_retirada IS NOT NULL AND LENGTH(TRIM(justificativa_retirada)) > 0
        AND reutilizavel = FALSE)
);
-- Retiradas legadas continuam sem autor/data/justificativa conhecidos. Não inventa evidência histórica.
