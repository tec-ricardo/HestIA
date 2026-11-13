CREATE TABLE processamento_avaliacao (
 registro_id BIGINT PRIMARY KEY REFERENCES registros_uso_ia(id),
 empresa_id BIGINT NOT NULL REFERENCES empresas(id),
 estado VARCHAR(30) NOT NULL,
 resultado VARCHAR(30),
 avaliacao_id BIGINT REFERENCES avaliacoes_conformidade(id),
 snapshot_ferramenta VARCHAR(30) NOT NULL,
 politica_versao_id BIGINT REFERENCES politica_uso_versoes(id),
 capturado_em TIMESTAMP NOT NULL,
 motivo VARCHAR(2000),
 evidencias TEXT,
 tentativas INT NOT NULL DEFAULT 0,
 atualizado_em TIMESTAMP NOT NULL
);
CREATE TABLE tentativas_avaliacao (
 id BIGSERIAL PRIMARY KEY,
 registro_id BIGINT NOT NULL REFERENCES processamento_avaliacao(registro_id),
 tentativa INT NOT NULL,
 estado VARCHAR(30) NOT NULL,
 registrada_em TIMESTAMP NOT NULL,
 UNIQUE(registro_id,tentativa)
);
