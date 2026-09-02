CREATE TABLE empresas (
    id BIGSERIAL PRIMARY KEY,
    nome VARCHAR(150) NOT NULL,
    cnpj VARCHAR(14) NOT NULL UNIQUE,
    configuracoes_gerais VARCHAR(255),
    orcamento DOUBLE PRECISION
);

CREATE TABLE departamentos (
    id BIGSERIAL PRIMARY KEY,
    nome VARCHAR(255) NOT NULL,
    responsavel VARCHAR(255),
    estrutura_hierarquica VARCHAR(255),
    empresa_id BIGINT NOT NULL REFERENCES empresas(id)
);

CREATE TABLE usuarios (
    id BIGSERIAL PRIMARY KEY,
    nome VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    senha VARCHAR(255) NOT NULL,
    cargo VARCHAR(100) NOT NULL,
    perfil VARCHAR(40) NOT NULL,
    ativo BOOLEAN NOT NULL,
    data_cadastro TIMESTAMP NOT NULL,
    empresa_id BIGINT NOT NULL REFERENCES empresas(id),
    departamento_id BIGINT NOT NULL REFERENCES departamentos(id)
);

CREATE TABLE politicas_uso (
    id BIGSERIAL PRIMARY KEY,
    titulo VARCHAR(150) NOT NULL,
    descricao VARCHAR(500) NOT NULL,
    conteudo TEXT NOT NULL,
    versao VARCHAR(20) NOT NULL,
    ativa BOOLEAN NOT NULL,
    data_criacao TIMESTAMP NOT NULL,
    data_atualizacao TIMESTAMP NOT NULL,
    empresa_id BIGINT NOT NULL REFERENCES empresas(id)
);

CREATE TABLE ferramentas_ia (
    id BIGSERIAL PRIMARY KEY,
    nome VARCHAR(120) NOT NULL,
    fornecedor VARCHAR(120) NOT NULL,
    descricao VARCHAR(500),
    tipo VARCHAR(40) NOT NULL,
    finalidade_uso VARCHAR(500) NOT NULL,
    url_acesso VARCHAR(2048),
    status VARCHAR(30) NOT NULL,
    nivel_risco VARCHAR(30) NOT NULL,
    trata_dados_pessoais BOOLEAN NOT NULL,
    data_cadastro TIMESTAMP NOT NULL,
    data_atualizacao TIMESTAMP NOT NULL,
    empresa_id BIGINT NOT NULL REFERENCES empresas(id),
    CONSTRAINT uk_ferramenta_empresa_nome_fornecedor
        UNIQUE (empresa_id, nome, fornecedor)
);

CREATE INDEX idx_departamento_empresa ON departamentos(empresa_id);
CREATE INDEX idx_usuario_empresa ON usuarios(empresa_id);
CREATE INDEX idx_politica_empresa ON politicas_uso(empresa_id);
CREATE INDEX idx_ferramenta_empresa ON ferramentas_ia(empresa_id);
CREATE INDEX idx_ferramenta_status ON ferramentas_ia(status);
