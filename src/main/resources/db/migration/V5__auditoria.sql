CREATE TABLE logs_auditoria (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT,
    empresa_id BIGINT,
    metodo VARCHAR(20) NOT NULL,
    recurso VARCHAR(500) NOT NULL,
    acao VARCHAR(40) NOT NULL,
    status_http INTEGER NOT NULL,
    endereco_ip VARCHAR(64),
    data_hora TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_auditoria_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id),
    CONSTRAINT fk_auditoria_empresa FOREIGN KEY (empresa_id) REFERENCES empresas(id)
);

CREATE INDEX idx_auditoria_empresa_data ON logs_auditoria(empresa_id, data_hora);
CREATE INDEX idx_auditoria_usuario ON logs_auditoria(usuario_id);
