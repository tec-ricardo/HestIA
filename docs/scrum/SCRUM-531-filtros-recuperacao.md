# SCRUM-531 — Filtros de empresa e perfil

A recuperação usa empresa e perfil do usuário autenticado pelo interceptor existente.
Uma empresa diferente na URL retorna 403, inclusive para ADMIN. A consulta filtra
empresa, perfil explicitamente permitido e elegibilidade antes do limite de resultados.
As mesmas regras protegem listagens, consulta por ID e reutilização pela API.

## Permissões

Cada resposta tem um conjunto de perfis permitidos, sem hierarquia implícita.
Novos cadastros recebem o perfil do autor autenticado; empresa e autor são validados
no servidor. Perfis informados pelo cliente não ampliam o acesso.
Respostas antigas ficam indisponíveis até autorização explícita pelo ADMIN da empresa.

`PUT /api/respostas-reutilizaveis/{id}/perfis`, com Bearer token de ADMIN:

```json
["FUNCIONARIO", "GESTOR"]
```

O conjunto vazio revoga todo acesso de recuperação. ADMIN também precisa constar
na lista para recuperar o conteúdo, mas pode administrar permissões na própria empresa.
A migração V14 cria a tabela de permissões sem liberar automaticamente conteúdo antigo.

## Limites da implementação existente

A recuperação desta tarefa usa embeddings em TEXT e consultas textuais/candidatos.
Após a integração com develop, existe também BuscaVetorialService com pgvector.
Esse caminho vetorial agora resolve o usuário autenticado pelo contexto no servidor,
valida vínculo e atividade e filtra permissões de perfil no SQL antes de ordenar e
limitar resultados. A indexação também exige permissão explícita; ADMIN não recebe
acesso implícito. A montagem final dos resultados verifica novamente a permissão.
O fluxo RAG deve consumir exclusivamente a recuperação autorizada e tratar
lista vazia sem ampliar o escopo nem enviar conteúdo restrito ao modelo.

## Validação

RecuperacaoPerguntasSemelhantesTest testa consultas reais em H2: duas empresas,
dois perfis, dados sensíveis, bloqueio, ausência de permissões, consulta por ID,
limite após filtros, tentativa de trocar empresa e autorização/revogação por ADMIN.
FlywayMigrationTest verifica a aplicação sequencial das 14 migrações.
O teste integraContextoDaEmpresaComFiltroDePerfil verifica a conciliação entre
EmpresaAtual da develop e AcessoRespostaService desta tarefa.
BuscaVetorialPostgresTest executa migrações e consultas reais em PostgreSQL/pgvector,
com embeddings determinísticos e repositórios de usuário/entidade simulados.
O teste comprova o filtro antes do LIMIT, ausência de permissão de ADMIN,
bloqueio de usuário inativo/empresa diferente e autorização de indexação.
