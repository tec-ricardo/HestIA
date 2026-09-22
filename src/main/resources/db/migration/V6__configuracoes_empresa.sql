CREATE TABLE configuracoes_empresa (
    id BIGSERIAL PRIMARY KEY,
    empresa_id BIGINT NOT NULL UNIQUE,
    registrar_usos_automaticamente BOOLEAN NOT NULL DEFAULT TRUE,
    bloquear_ferramentas_nao_aprovadas BOOLEAN NOT NULL DEFAULT TRUE,
    limite_custo_mensal NUMERIC(15, 2),
    dias_retencao_auditoria INTEGER NOT NULL DEFAULT 365,
    atualizada_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_configuracao_empresa FOREIGN KEY (empresa_id) REFERENCES empresas(id),
    CONSTRAINT ck_configuracao_limite CHECK (limite_custo_mensal IS NULL OR limite_custo_mensal >= 0),
    CONSTRAINT ck_configuracao_retencao CHECK (dias_retencao_auditoria > 0)
);
