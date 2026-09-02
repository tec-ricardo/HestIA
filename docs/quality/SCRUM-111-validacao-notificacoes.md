# SCRUM-111 - Validacao de envio e consulta de notificacoes

## Cobertura automatizada

| Cenario | Evidencia |
|---|---|
| criacao e classificacao | testes do `alert-service` |
| entrada invalida | testes do `alert-service` |
| recurso inexistente | testes do `alert-service` |
| idempotencia | testes do `alert-service` e teste ponta a ponta |
| consulta e filtro por empresa | teste ponta a ponta |
| resposta HTTP valida | `AlertServiceClientTest` |
| indisponibilidade | `AlertServiceClientTest` |
| inicializacao do Spring | `HestIaApplicationTests` |

## Execucao

1. Em `alert-service`, executar `npm ci` e `npm test`.
2. Iniciar o Node com `npm start`.
3. Executar Maven com `ALERT_E2E=true` e
   `ALERT_SERVICE_URL=http://127.0.0.1:3001`.

O teste ponta a ponta fica desabilitado quando `ALERT_E2E` nao e informado,
evitando que a suite unitaria dependa de um processo externo.

## Resultado esperado

- a primeira chamada cria uma notificacao critica;
- repetir a chave idempotente devolve o mesmo identificador;
- a consulta individual encontra o registro;
- a listagem por empresa contem o registro criado;
- falhas externas sao convertidas em `AlertServiceException`.
