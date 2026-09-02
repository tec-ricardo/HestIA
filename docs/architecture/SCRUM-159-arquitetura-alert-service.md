# SCRUM-159 - Arquitetura do Alert Service em Node.js

## Decisao

O Alert Service sera um componente auxiliar academico, executado como processo
separado apenas no ambiente de demonstracao. O dominio e os dados canonicos
continuam no monolito modular Spring Boot. Esta excecao nao inicia uma migracao
para microsservicos e nao transfere propriedade de dados corporativos ao Node.js.

## Responsabilidade

O servico recebe um evento ja validado pelo backend principal, classifica o
nivel do alerta e guarda uma notificacao minima para consulta durante a
demonstracao.

Ele nao autentica usuarios, nao acessa tabelas do monolito, nao decide
permissoes e nao armazena senha, token ou conteudo sensivel.

## Contrato HTTP

O contrato versionado esta em `alert-service-openapi.yaml`.

| Operacao | Finalidade |
|---|---|
| `POST /api/v1/notifications` | cria e classifica uma notificacao |
| `GET /api/v1/notifications/{id}` | consulta por identificador |
| `GET /api/v1/notifications?empresaId=` | lista por empresa |
| `GET /health` | informa disponibilidade do processo |

Toda requisicao de criacao possui `empresaId`, `origem`, `tipo`, `valor`,
`mensagem` e `idempotencyKey`. A resposta possui `id`, `nivel`, `status`, datas
e `correlationId`.

## Classificacao inicial

- `valor >= 90`: `CRITICAL`;
- `valor >= 70`: `WARNING`;
- demais valores: `INFO`.

As faixas sao configuraveis e servem apenas ao MVP. Regras futuras devem ser
versionadas e testadas, sem alterar retroativamente notificacoes existentes.

## Persistencia

O MVP usa um repositorio em memoria, com interface propria, para manter o foco
no contrato e nos testes. Reiniciar o processo pode apagar os registros. Uma
persistencia independente so deve ser adotada com requisito explicito.

## Confiabilidade e falhas

- timeout do cliente Spring: 2 segundos;
- repeticao da mesma `idempotencyKey` devolve a notificacao existente;
- JSON invalido ou campo ausente retorna `400`;
- recurso inexistente retorna `404`;
- erro inesperado retorna `500` sem detalhes internos;
- logs incluem `correlationId`, metodo, rota e duracao;
- falha no Node nao desfaz a operacao principal do monolito.

## Configuracao

| Variavel | Padrao | Uso |
|---|---|---|
| `PORT` | `3001` | porta HTTP |
| `LOG_LEVEL` | `info` | nivel de log |
| `WARNING_THRESHOLD` | `70` | inicio de alerta amarelo |
| `CRITICAL_THRESHOLD` | `90` | inicio de alerta critico |

## Testes obrigatorios

- criacao nos tres niveis;
- validacao de entrada;
- consulta existente e inexistente;
- filtro por empresa;
- idempotencia;
- health check;
- contrato do adaptador Spring;
- indisponibilidade, timeout e resposta invalida.

## Criterios de aceite

- limite entre Java e Node explicito;
- OpenAPI versionada;
- falhas e idempotencia definidas;
- configuracao e observabilidade documentadas;
- implementacao da SCRUM-109 nao depende de decisoes arquiteturais abertas.
