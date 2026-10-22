ALTER TABLE respostas_reutilizaveis ADD COLUMN status_qualidade VARCHAR(30) NOT NULL DEFAULT 'APROVADA';
ALTER TABLE respostas_reutilizaveis ADD COLUMN expira_em TIMESTAMP;
UPDATE respostas_reutilizaveis SET status_qualidade='RETIRADA' WHERE reutilizavel=false;
-- Compatibilidade: APROVADA preserva a elegibilidade legada já sujeita a flags e perfis explícitos.
-- Novos cadastros pelo serviço iniciam EM_REVISAO; não atribuir revisor humano a registros legados.
CREATE TABLE resposta_revisoes (
 id BIGSERIAL PRIMARY KEY, resposta_id BIGINT NOT NULL REFERENCES respostas_reutilizaveis(id),
 empresa_id BIGINT NOT NULL REFERENCES empresas(id), responsavel_id BIGINT NOT NULL REFERENCES usuarios(id),
 estado_anterior VARCHAR(30) NOT NULL, estado_atual VARCHAR(30) NOT NULL, justificativa VARCHAR(1000) NOT NULL,
 revisada_em TIMESTAMP NOT NULL, conteudo_sha256 VARCHAR(64) NOT NULL, versao_conteudo INTEGER NOT NULL,
 expira_em TIMESTAMP
);
CREATE TABLE decisoes_recuperacao (
 id BIGSERIAL PRIMARY KEY, empresa_id BIGINT NOT NULL, usuario_id BIGINT NOT NULL, resposta_id BIGINT,
 recurso VARCHAR(80) NOT NULL, decisao VARCHAR(40) NOT NULL, motivo VARCHAR(100) NOT NULL, regra_versao VARCHAR(50) NOT NULL,
 registrada_em TIMESTAMP NOT NULL
);
CREATE INDEX idx_decisao_empresa_data ON decisoes_recuperacao(empresa_id,registrada_em);
