CREATE TABLE resposta_perfis (
    resposta_id BIGINT NOT NULL REFERENCES respostas_reutilizaveis(id) ON DELETE CASCADE,
    perfil VARCHAR(255) NOT NULL,
    PRIMARY KEY (resposta_id, perfil),
    CONSTRAINT ck_resposta_perfil CHECK (perfil IN ('FUNCIONARIO', 'GESTOR', 'ADMIN'))
);
-- Sem preenchimento automático: respostas antigas exigem autorização explícita.
