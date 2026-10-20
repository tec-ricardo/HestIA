# Migrações e integração do Deliverable 2

Em 20/10/2026 a develop foi integrada na branch de trabalho SCRUM-531.
As alterações locais foram preservadas e os conflitos conciliados.

## Numeração consolidada

| Versão | Finalidade |
|---|---|
| V1 a V10 | Migrações existentes, sem alteração |
| V11 | pgvector (Java; execução específica de PostgreSQL) |
| V12 | Recuperação de senha (SQL) |
| V13 | Staging da base sintética (Java; antes colidia com V12 SQL) |
| V14 | Permissões de recuperação (SQL local; antes colidia com V11 Java) |

O teste de migração usa uma base H2 efêmera e verifica 14 versões e as tabelas
relacionais. Os corpos PostgreSQL das migrações Java não são executados em H2;
esse teste não valida instalação de pgvector nem criação do índice HNSW.

## Validação PostgreSQL/pgvector

`BuscaVetorialPostgresTest` é a validação adicional específica do PostgreSQL.
Usa um banco descartável, migra um schema único por teste e verifica extensão,
índice HNSW, staging, 14 versões e reaplicação sem migrações pendentes.
As consultas vetoriais exercitam permissões antes do LIMIT e indexação.
Embeddings são vetores determinísticos de 1.536 dimensões; não há chamadas
ao provedor externo. Os repositórios de entidades e usuários são simulados;
o SQL, o driver, o Flyway e o banco vetorial são reais.

Definir `HESTIA_PG_TEST_URL`, `HESTIA_PG_TEST_USER` e
`HESTIA_PG_TEST_PASSWORD` para um banco **descartável** antes de executar
`mvnw test`. Não apontar esse teste para dados corporativos. O teste cria
schemas temporários e a migração de staging cria `base_sintetica`.
O workflow CI provisiona seu próprio serviço `pgvector/pgvector:pg17`.

## Bancos existentes

Nenhum banco persistente foi modificado nesta tarefa. Antes de aplicar esta
versão a um banco existente, inspecionar `flyway_schema_history`. Se já houver
V11 de permissões ou V12 de staging aplicada, interromper a atualização e
planejar conciliação específica do histórico com backup. Não usar `repair`,
apagar histórico ou recriar banco automaticamente.

## Build no Windows

O padrão de compilação permanece `target`. Em pasta sincronizada que bloqueie
a limpeza, usar `-Dhestia.build.directory=<diretorio-temporario-absoluto>`.
Isso move apenas os produtos de compilação; os fontes continuam no projeto.
