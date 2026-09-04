# HestIA

HestIA e uma plataforma corporativa para governanca, monitoramento e uso
responsavel de Inteligencia Artificial, desenvolvida no Projeto Integrado IV
da ESPM.

## Requisitos

- Java 21;
- Docker Desktop, para o PostgreSQL local.

## Executar localmente

1. Inicie o banco e o Alert Service com `docker compose up -d`.
2. No Windows, execute `mvnw.cmd spring-boot:run`.
3. No Linux ou macOS, execute `./mvnw spring-boot:run`.

Em outro terminal, inicie o front-end integrado:

```text
cd Front-End/hestia-prototipo-sprint1-atualizado
npm install
npm start
```

O front fica disponível em `http://localhost:3000/acesso` e utiliza Axios no
servidor Express para acessar a API configurada em `API_URL` (por padrão,
`http://localhost:8080`). Na tela de Administração é possível cadastrar
empresas, departamentos, usuários e ferramentas de IA; a tela de Governança
persiste e consulta políticas de uso.

Com a aplicacao iniciada, a documentacao interativa da API fica disponivel em
`http://localhost:8080/swagger-ui/index.html`.

Os valores locais padrao correspondem ao `compose.yaml`. Para usar outro banco,
configure `DB_URL`, `DB_USERNAME` e `DB_PASSWORD` conforme `.env.example`.
Quando a porta `5432` ja estiver ocupada, crie um arquivo `.env` com
`POSTGRES_PORT=5433` e configure `DB_URL=jdbc:postgresql://localhost:5433/hestia`
no processo Java. O arquivo `.env` e local e nao deve ser versionado.

## Testes

Execute `mvnw.cmd test` no Windows ou `./mvnw test` no Linux/macOS. Os testes
usam um banco H2 isolado e nao exigem PostgreSQL.

O mesmo comando e executado automaticamente pelo pipeline em Pull Requests.

O Alert Service possui testes proprios em `alert-service` com `npm test`. O
cenario matematico pode ser validado em `model/optimization` com
`python -m unittest -v`.
