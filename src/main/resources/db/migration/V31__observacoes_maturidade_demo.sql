CREATE TABLE maturidade_observacoes_demo (
 id BIGSERIAL PRIMARY KEY,
 empresa_id BIGINT NOT NULL REFERENCES empresas(id),
 departamento_id BIGINT NOT NULL REFERENCES departamentos(id),
 versao VARCHAR(40) NOT NULL,
 chave VARCHAR(150) NOT NULL,
 tipo VARCHAR(30) NOT NULL CHECK(tipo IN ('TREINAMENTO','USO','ISSUE','PR','FINANCEIRO','CONTROLE')),
 data_referencia DATE NOT NULL,
 natureza VARCHAR(20) NOT NULL CHECK(natureza='SIMULADA'),
 dados TEXT NOT NULL,
 UNIQUE(empresa_id,versao,chave)
);
CREATE INDEX idx_maturidade_demo_escopo ON maturidade_observacoes_demo(empresa_id,departamento_id,data_referencia);
