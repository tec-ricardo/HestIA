CREATE TABLE progressos_usuario (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT NOT NULL UNIQUE,
    xp_total INTEGER NOT NULL DEFAULT 0,
    nivel VARCHAR(30) NOT NULL DEFAULT 'APRENDIZ',
    atualizado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_progresso_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id),
    CONSTRAINT ck_progresso_xp CHECK (xp_total >= 0)
);

CREATE TABLE movimentos_experiencia (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT NOT NULL,
    acao VARCHAR(40) NOT NULL,
    pontos INTEGER NOT NULL,
    referencia VARCHAR(150) NOT NULL,
    data_hora TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_movimento_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id),
    CONSTRAINT uk_movimento_referencia UNIQUE (usuario_id, referencia)
);

CREATE TABLE missoes (
    id BIGSERIAL PRIMARY KEY,
    codigo VARCHAR(80) NOT NULL UNIQUE,
    titulo VARCHAR(150) NOT NULL,
    descricao VARCHAR(500) NOT NULL,
    acao_alvo VARCHAR(40) NOT NULL,
    meta_quantidade INTEGER NOT NULL,
    recompensa_xp INTEGER NOT NULL,
    ativa BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT ck_missao_meta CHECK (meta_quantidade > 0),
    CONSTRAINT ck_missao_recompensa CHECK (recompensa_xp >= 0)
);

CREATE TABLE progressos_missao (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT NOT NULL,
    missao_id BIGINT NOT NULL,
    progresso INTEGER NOT NULL DEFAULT 0,
    concluida_em TIMESTAMP,
    recompensa_concedida BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_progresso_missao_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id),
    CONSTRAINT fk_progresso_missao FOREIGN KEY (missao_id) REFERENCES missoes(id),
    CONSTRAINT uk_progresso_missao UNIQUE (usuario_id, missao_id)
);

INSERT INTO missoes (codigo, titulo, descricao, acao_alvo, meta_quantidade, recompensa_xp) VALUES
('PRIMEIROS_REGISTROS', 'Uso consciente', 'Registre cinco utilizações de ferramentas de IA.', 'REGISTRO_USO', 5, 50),
('CONFORMIDADE_EM_DIA', 'Conformidade em dia', 'Conclua três usos avaliados como conformes.', 'USO_CONFORME', 3, 75),
('REUTILIZACAO_EFICIENTE', 'Reutilização eficiente', 'Reutilize dez respostas para evitar novas chamadas de IA.', 'REUTILIZACAO_RESPOSTA', 10, 100);
